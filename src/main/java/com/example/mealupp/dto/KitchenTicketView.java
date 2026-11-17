package com.example.mealupp.dto;

import com.example.mealupp.model.KitchenTicket;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.model.TicketStatus;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Comanda tal como la ve la cocina: qué preparar, cuánto y para qué hora. Sin datos del cliente.
 */
public record KitchenTicketView(
    Long ticketId,
    Long orderId,
    String dishName,
    int quantity,
    LocalTime deliveryTime,
    OrderStatus orderStatus,
    TicketStatus ticketStatus) {

  public static KitchenTicketView from(KitchenTicket ticket) {
    Order order = ticket.getCustomerOrder();
    return new KitchenTicketView(
        ticket.getId(),
        order.getId(),
        order.getDish().getName(),
        order.getQuantity(),
        order.getDeliveryTime(),
        order.getStatus(),
        ticket.getStatus());
  }

  /** Datos del mensaje que se publica por WebSocket. "event" es CREATED o UPDATED. */
  public Map<String, Object> asMap(String event) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("event", event);
    map.put("ticketId", ticketId);
    map.put("orderId", orderId);
    map.put("dishName", dishName);
    map.put("quantity", quantity);
    map.put("deliveryTime", deliveryTime);
    map.put("orderStatus", orderStatus);
    map.put("ticketStatus", ticketStatus);
    return map;
  }
}
