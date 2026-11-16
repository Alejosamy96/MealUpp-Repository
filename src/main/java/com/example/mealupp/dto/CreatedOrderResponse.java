package com.example.mealupp.dto;

import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Respuesta al crear un pedido. "accessCode" sirve para consultar el pedido sin iniciar sesión y
 * "paymentUrl" es la dirección a la que hay que llevar al cliente a pagar (nula si no se paga en
 * línea).
 */
public record CreatedOrderResponse(
    Long id,
    String dishName,
    int quantity,
    String customerName,
    LocalDate orderDate,
    LocalTime deliveryTime,
    DeliveryZone deliveryZone,
    String deliveryDetail,
    OrderStatus status,
    BigDecimal total,
    String accessCode,
    String paymentUrl) {

  public static CreatedOrderResponse from(Order order, String paymentUrl) {
    return new CreatedOrderResponse(
        order.getId(),
        order.getDish().getName(),
        order.getQuantity(),
        order.getCustomerName(),
        order.getOrderDate(),
        order.getDeliveryTime(),
        order.getDeliveryZone(),
        order.getDeliveryDetail(),
        order.getStatus(),
        order.getTotal(),
        order.getAccessCode(),
        paymentUrl);
  }
}
