package com.example.mealupp.dto;

import com.example.mealupp.model.KitchenTicket;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.model.TicketStatus;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Comanda tal como la ve la cocina: qué preparar, cuánto y para qué hora. Sin datos del cliente;
 * solo se sabe si ya llegó a recoger (customerArrived).
 */
public record KitchenTicketView(
    Long ticketId,
    Long orderId,
    String dishName,
    int quantity,
    LocalTime deliveryTime,
    OrderStatus orderStatus,
    TicketStatus ticketStatus,
    boolean customerArrived,
    List<OrderItemView> items) {

  public static KitchenTicketView from(KitchenTicket ticket) {
    Order order = ticket.getCustomerOrder();
    return new KitchenTicketView(
        ticket.getId(),
        order.getId(),
        order.dishSummary(),
        order.getQuantity(),
        order.getDeliveryTime(),
        order.getStatus(),
        ticket.getStatus(),
        order.getArrivedAt() != null,
        OrderItemView.listOf(order));
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
    map.put("customerArrived", customerArrived);
    map.put("items", OrderItemView.asMaps(items));
    return map;
  }
}
