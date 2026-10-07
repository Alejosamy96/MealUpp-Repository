package com.example.mealupp.model;

/**
 * Estados de un pedido. Transiciones permitidas: PENDING -> IN_PREPARATION -> DELIVERED, y
 * PENDING -> CANCELLED.
 */
public enum OrderStatus {
  PENDING,
  IN_PREPARATION,
  DELIVERED,
  CANCELLED;

  public boolean canTransitionTo(OrderStatus next) {
    return switch (this) {
      case PENDING -> next == IN_PREPARATION || next == CANCELLED;
      case IN_PREPARATION -> next == DELIVERED;
      case DELIVERED, CANCELLED -> false;
    };
  }

  public boolean isFinal() {
    return this == DELIVERED || this == CANCELLED;
  }
}
