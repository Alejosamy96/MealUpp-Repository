package com.example.mealupp.integration.notification;

import com.example.mealupp.dto.KitchenTicketView;
import com.example.mealupp.dto.OrderResponse;
import com.example.mealupp.util.MiniJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los avisos por WebSocket (STOMP). El cuerpo de cada mensaje es un texto JSON. Si el canal
 * falla no se interrumpe el pedido: los datos quedan guardados y las pantallas los recuperan por
 * REST al reconectar.
 */
@Component
public class WebSocketKitchenNotifier implements KitchenNotifier {

  public static final String TOPIC_TICKETS = "/topic/comandas";
  public static final String TOPIC_ORDERS = "/topic/pedidos";

  private static final Logger LOG = LoggerFactory.getLogger(WebSocketKitchenNotifier.class);

  private final SimpMessagingTemplate template;

  public WebSocketKitchenNotifier(SimpMessagingTemplate template) {
    this.template = template;
  }

  @Override
  public void ticketCreated(KitchenTicketView ticket) {
    publish(TOPIC_TICKETS, MiniJson.write(ticket.asMap("CREATED")));
  }

  @Override
  public void ticketUpdated(KitchenTicketView ticket) {
    publish(TOPIC_TICKETS, MiniJson.write(ticket.asMap("UPDATED")));
  }

  @Override
  public void orderChanged(OrderResponse order) {
    publish(TOPIC_ORDERS, MiniJson.write(order.asMap()));
  }

  private void publish(String destination, String json) {
    try {
      template.convertAndSend(destination, json);
    } catch (RuntimeException ex) {
      LOG.warn("No se pudo publicar en {}: {}", destination, ex.getMessage());
    }
  }
}
