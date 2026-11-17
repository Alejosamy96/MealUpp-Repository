package com.example.mealupp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.mealupp.model.StaffRole;
import com.example.mealupp.service.StaffAuthService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/** Seguridad del WebSocket: quién puede conectarse y a qué canales puede suscribirse. */
@ExtendWith(MockitoExtension.class)
class StompAuthInterceptorTest {

  @Mock private StaffAuthService authService;

  private StompAuthInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new StompAuthInterceptor(authService);
  }

  private static Message<byte[]> connect(String authorization) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    if (authorization != null) {
      accessor.addNativeHeader("Authorization", authorization);
    }
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private static Message<byte[]> subscribe(String destination, StaffPrincipal principal) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(destination);
    if (principal != null) {
      accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  @Test
  void connectingWithAValidTokenIdentifiesThePerson() {
    when(authService.findPrincipalByToken("token-bueno"))
        .thenReturn(Optional.of(new StaffPrincipal("cocina1", StaffRole.KITCHEN)));
    Message<byte[]> message = connect("Bearer token-bueno");

    interceptor.preSend(message, null);

    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    assertThat(accessor).isNotNull();
    assertThat(accessor.getUser()).isNotNull();
    assertThat(accessor.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
    assertThat(((UsernamePasswordAuthenticationToken) accessor.getUser()).getPrincipal())
        .isEqualTo(new StaffPrincipal("cocina1", StaffRole.KITCHEN));
  }

  @Test
  void connectingWithoutAHeaderIsRejected() {
    Message<byte[]> message = connect(null);

    assertThatThrownBy(() -> interceptor.preSend(message, null))
        .isInstanceOf(MessagingException.class)
        .hasMessage("No autorizado");
  }

  @Test
  void connectingWithAHeaderThatIsNotBearerIsRejected() {
    Message<byte[]> message = connect("Basic abc");

    assertThatThrownBy(() -> interceptor.preSend(message, null))
        .isInstanceOf(MessagingException.class);
  }

  @Test
  void connectingWithAnInvalidTokenIsRejected() {
    when(authService.findPrincipalByToken("falso")).thenReturn(Optional.empty());
    Message<byte[]> message = connect("Bearer falso");

    assertThatThrownBy(() -> interceptor.preSend(message, null))
        .isInstanceOf(MessagingException.class)
        .hasMessage("No autorizado");
  }

  @ParameterizedTest(name = "{0} en {1} permitido={2}")
  @CsvSource({
    "KITCHEN,  /topic/comandas, true",
    "ADMIN,    /topic/comandas, true",
    "CASHIER,  /topic/comandas, false",
    "DELIVERY, /topic/comandas, false",
    "ADMIN,    /topic/pedidos,  true",
    "CASHIER,  /topic/pedidos,  true",
    "DELIVERY, /topic/pedidos,  true",
    "KITCHEN,  /topic/pedidos,  false",
    "ADMIN,    /topic/otro,     false",
    "ADMIN,    /user/queue/x,   false"
  })
  void eachRoleOnlyListensToItsChannels(StaffRole role, String destination, boolean allowed) {
    Message<byte[]> message = subscribe(destination, new StaffPrincipal("alguien", role));

    if (allowed) {
      assertThat(interceptor.preSend(message, null)).isSameAs(message);
    } else {
      assertThatThrownBy(() -> interceptor.preSend(message, null))
          .isInstanceOf(MessagingException.class)
          .hasMessage("No tienes permiso para esta suscripción");
    }
  }

  @Test
  void subscribingWithoutHavingConnectedIsRejected() {
    Message<byte[]> message = subscribe("/topic/comandas", null);

    assertThatThrownBy(() -> interceptor.preSend(message, null))
        .isInstanceOf(MessagingException.class)
        .hasMessage("No autorizado");
  }

  @Test
  void nobodyCanSendMessagesToTheServer() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    accessor.setDestination("/app/lo-que-sea");
    accessor.setLeaveMutable(true);
    Message<byte[]> message =
        MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    assertThatThrownBy(() -> interceptor.preSend(message, null))
        .isInstanceOf(MessagingException.class);
  }

  @Test
  void otherCommandsPassThrough() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
    accessor.setLeaveMutable(true);
    Message<byte[]> message =
        MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    assertThat(interceptor.preSend(message, null)).isSameAs(message);
  }
}
