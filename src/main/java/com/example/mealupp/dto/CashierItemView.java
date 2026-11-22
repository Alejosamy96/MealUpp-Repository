package com.example.mealupp.dto;

import com.example.mealupp.model.Order;
import java.math.BigDecimal;
import java.util.List;

/** Un plato de un pedido con su cantidad y precio, para caja. */
public record CashierItemView(String dishName, int quantity, BigDecimal unitPrice) {

  public static List<CashierItemView> listOf(Order order) {
    return order.lines().stream()
        .map(line -> new CashierItemView(line.dish().getName(), line.quantity(), line.unitPrice()))
        .toList();
  }
}
