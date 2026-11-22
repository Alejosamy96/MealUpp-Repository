package com.example.mealupp.integration.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.example.mealupp.dto.KitchenTicketView;
import com.example.mealupp.dto.OrderItemView;
import com.example.mealupp.dto.OrderResponse;
import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.model.TicketStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class WebSocketKitchenNotifierTest {

  @Mock private SimpMessagingTemplate template;

  private WebSocketKitchenNotifier notifier;
  private final KitchenTicketView ticket =
      new KitchenTicketView(
          4L,
          7L,
          "Pollo a la plancha",
          2,
          LocalTime.of(12, 30),
          OrderStatus.PENDING,
          TicketStatus.SENT,
          false,
          List.of(new OrderItemView("Pollo a la plancha", 2)));

  @BeforeEach
  void setUp() {
    notifier = new WebSocketKitchenNotifier(template);
  }

  @Test
  void aNewTicketIsPublishedInTheKitchenChannelAsJson() {
    notifier.ticketCreated(ticket);

    ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
    verify(template).convertAndSend(eq("/topic/comandas"), payload.capture());
    assertThat(payload.getValue().toString())
        .isEqualTo(
            "{\"event\":\"CREATED\",\"ticketId\":4,\"orderId\":7,\"dishName\":\"Pollo a la plancha\","
                + "\"quantity\":2,\"deliveryTime\":\"12:30\",\"orderStatus\":\"PENDING\","
                + "\"ticketStatus\":\"SENT\",\"customerArrived\":false,"
                + "\"items\":[{\"dishName\":\"Pollo a la plancha\",\"quantity\":2}]}");
  }

  @Test
  void anUpdatedTicketIsPublishedWithTheUpdatedEvent() {
    notifier.ticketUpdated(ticket);

    ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
    verify(template).convertAndSend(eq("/topic/comandas"), payload.capture());
    assertThat(payload.getValue().toString()).contains("\"event\":\"UPDATED\"");
  }

  @Test
  void aChangedOrderIsPublishedInTheOrdersChannel() {
    OrderResponse order =
        new OrderResponse(
            7L,
            "Pollo a la plancha",
            2,
            "Valentina",
            LocalDate.of(2026, 11, 4),
            LocalTime.of(12, 30),
            DeliveryZone.SHOPPING_CENTER,
            "Local 12",
            OrderStatus.IN_PREPARATION,
            new BigDecimal("24000"),
            List.of(new OrderItemView("Pollo a la plancha", 2)));

    notifier.orderChanged(order);

    ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
    verify(template).convertAndSend(eq("/topic/pedidos"), payload.capture());
    assertThat(payload.getValue().toString())
        .contains("\"event\":\"ORDER_CHANGED\"")
        .contains("\"status\":\"IN_PREPARATION\"")
        .contains("\"total\":24000")
        .contains("\"deliveryZone\":\"SHOPPING_CENTER\"")
        .contains("\"items\":[{\"dishName\":\"Pollo a la plancha\",\"quantity\":2}]");
  }

  @Test
  void aBrokenChannelDoesNotBreakTheCaller() {
    doThrow(new IllegalStateException("sin broker"))
        .when(template)
        .convertAndSend(anyString(), any(Object.class));

    assertThatCode(() -> notifier.ticketCreated(ticket)).doesNotThrowAnyException();
  }
}
