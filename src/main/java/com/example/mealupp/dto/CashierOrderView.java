package com.example.mealupp.dto;

import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import java.math.BigDecimal;
import java.util.List;

/** Lo que necesita caja: quién pidió, cuánto y cuánto cuesta. Sin el lugar de entrega. */
public record CashierOrderView(
    Long id,
    String customerName,
    String dishName,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal total,
    OrderStatus status,
    List<CashierItemView> items) {

  public static CashierOrderView from(Order order) {
    return new CashierOrderView(
        order.getId(),
        order.getCustomerName(),
        order.dishSummary(),
        order.getQuantity(),
        order.getUnitPrice(),
        order.getTotal(),
        order.getStatus(),
        CashierItemView.listOf(order));
  }
}
