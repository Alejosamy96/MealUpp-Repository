package com.example.mealupp.dto;

import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import java.time.LocalTime;

/** Lo que necesita quien entrega: a quién, dónde, a qué hora y qué lleva. Sin precios. */
public record DeliveryOrderView(
    Long id,
    String customerName,
    String dishName,
    int quantity,
    LocalTime deliveryTime,
    DeliveryZone deliveryZone,
    String deliveryDetail,
    OrderStatus status,
    boolean customerArrived) {

  public static DeliveryOrderView from(Order order) {
    return new DeliveryOrderView(
        order.getId(),
        order.getCustomerName(),
        order.getDish().getName(),
        order.getQuantity(),
        order.getDeliveryTime(),
        order.getDeliveryZone(),
        order.getDeliveryDetail(),
        order.getStatus(),
        order.getArrivedAt() != null);
  }
}
