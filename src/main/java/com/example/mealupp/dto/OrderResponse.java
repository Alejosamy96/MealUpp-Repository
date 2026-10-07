package com.example.mealupp.dto;

import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record OrderResponse(
    Long id,
    String dishName,
    int quantity,
    String customerName,
    LocalDate orderDate,
    LocalTime deliveryTime,
    DeliveryZone deliveryZone,
    String deliveryDetail,
    OrderStatus status) {

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
        order.getStatus());
  }
}
