package com.example.mealupp.dto;

import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import java.time.LocalTime;

/** Lo que necesita la cocina: qué preparar, cuánto y para qué hora. Sin datos del cliente. */
public record KitchenOrderView(
    Long id, String dishName, int quantity, LocalTime deliveryTime, OrderStatus status) {

  public static KitchenOrderView from(Order order) {
    return new KitchenOrderView(
        order.getId(),
        order.getDish().getName(),
        order.getQuantity(),
        order.getDeliveryTime(),
        order.getStatus());
  }
}
