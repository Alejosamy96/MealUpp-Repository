package com.example.mealupp.dto;

import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import java.math.BigDecimal;

/** Lo que necesita caja: quién pidió, cuánto y cuánto cuesta. Sin el lugar de entrega. */
public record CashierOrderView(
    Long id,
    String customerName,
    String dishName,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal total,
    OrderStatus status) {

  public static CashierOrderView from(Order order) {
    BigDecimal price = order.getDish().getPrice();
    return new CashierOrderView(
        order.getId(),
        order.getCustomerName(),
        order.getDish().getName(),
        order.getQuantity(),
        price,
        price.multiply(BigDecimal.valueOf(order.getQuantity())),
        order.getStatus());
  }
}
