package com.example.mealupp.dto;

import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/** Lo que ve el cliente al consultar su pedido con el código de acceso. */
public record OrderTrackingResponse(
    Long id,
    OrderStatus status,
    String dishName,
    int quantity,
    LocalTime deliveryTime,
    DeliveryZone deliveryZone,
    String deliveryDetail,
    BigDecimal total,
    boolean paymentPending,
    boolean arrived,
    List<OrderItemView> items) {

  public static OrderTrackingResponse from(Order order) {
    return new OrderTrackingResponse(
        order.getId(),
        order.getStatus(),
        order.dishSummary(),
        order.getQuantity(),
        order.getDeliveryTime(),
        order.getDeliveryZone(),
        order.getDeliveryDetail(),
        order.getTotal(),
        order.getStatus() == OrderStatus.PENDING_PAYMENT,
        order.getArrivedAt() != null,
        OrderItemView.listOf(order));
  }
}
