package com.example.mealupp.security;

import com.example.mealupp.model.StaffRole;
import com.example.mealupp.service.StaffAuthService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Seguridad del WebSocket. Al conectarse (CONNECT) la pantalla manda su token en el encabezado
 * Authorization; al suscribirse (SUBSCRIBE) solo puede escuchar los canales de su rol. Nadie puede
 * enviar mensajes al servidor por este canal: solo recibe avisos.
 */
public class StompAuthInterceptor implements ChannelInterceptor {

  private static final String PREFIX = "Bearer ";

  private static final Map<String, Set<StaffRole>> ALLOWED =
      Map.of(
          "/topic/comandas", Set.of(StaffRole.KITCHEN, StaffRole.ADMIN),
          "/topic/pedidos", Set.of(StaffRole.ADMIN, StaffRole.CASHIER, StaffRole.DELIVERY));

  private final StaffAuthService authService;

  public StompAuthInterceptor(StaffAuthService authService) {
    this.authService = authService;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() == null) {
      return message;
    }
    StompCommand command = accessor.getCommand();
    if (command == StompCommand.CONNECT) {
      authenticate(accessor);
    } else if (command == StompCommand.SUBSCRIBE) {
      authorizeSubscription(accessor);
    } else if (command == StompCommand.SEND) {
      throw new MessagingException("No se permite enviar mensajes por este canal");
    }
    return message;
  }

  private void authenticate(StompHeaderAccessor accessor) {
    String header = accessor.getFirstNativeHeader("Authorization");
    if (header == null || !header.startsWith(PREFIX)) {
      throw new MessagingException("No autorizado");
    }
    StaffPrincipal principal =
        authService
            .findPrincipalByToken(header.substring(PREFIX.length()))
            .orElseThrow(() -> new MessagingException("No autorizado"));
    accessor.setUser(
        new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))));
  }

  private void authorizeSubscription(StompHeaderAccessor accessor) {
    if (!(accessor.getUser() instanceof Authentication authentication)
        || !(authentication.getPrincipal() instanceof StaffPrincipal principal)) {
      throw new MessagingException("No autorizado");
    }
    Set<StaffRole> allowed = ALLOWED.get(accessor.getDestination());
    if (allowed == null || !allowed.contains(principal.role())) {
      throw new MessagingException("No tienes permiso para esta suscripción");
    }
  }
}
