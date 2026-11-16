package com.example.mealupp.dto;

import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record OrderResponse(
    Long id,
    String dishName,
    int quantity,
    String customerName,
    LocalDate orderDate,
    LocalTime deliveryTime,
    DeliveryZone deliveryZone,
    String deliveryDetail,
    OrderStatus status,
    BigDecimal total) {

  public static OrderResponse from(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getDish().getName(),
        order.getQuantity(),
        order.getCustomerName(),
        order.getOrderDate(),
        order.getDeliveryTime(),
        order.getDeliveryZone(),
        order.getDeliveryDetail(),
        order.getStatus(),
        order.getTotal());
  }

  /** Datos del mensaje que se publica por WebSocket cuando cambia un pedido. */
  public Map<String, Object> asMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("event", "ORDER_CHANGED");
    map.put("id", id);
    map.put("dishName", dishName);
    map.put("quantity", quantity);
    map.put("customerName", customerName);
    map.put("orderDate", orderDate);
    map.put("deliveryTime", deliveryTime);
    map.put("deliveryZone", deliveryZone);
    map.put("deliveryDetail", deliveryDetail);
    map.put("status", status);
    map.put("total", total);
    return map;
  }
}
