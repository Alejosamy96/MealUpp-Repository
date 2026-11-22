package com.example.mealupp.model;

import java.math.BigDecimal;

/** Una línea de un pedido: cuántos almuerzos de un plato y a qué precio se pidieron. */
public record OrderLine(Dish dish, int quantity, BigDecimal unitPrice) {

  /** Línea con el precio actual del plato (cero si el plato no tiene precio). */
  public static OrderLine of(Dish dish, int quantity) {
    return new OrderLine(
        dish, quantity, dish.getPrice() == null ? BigDecimal.ZERO : dish.getPrice());
  }

  public BigDecimal subtotal() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }
}
