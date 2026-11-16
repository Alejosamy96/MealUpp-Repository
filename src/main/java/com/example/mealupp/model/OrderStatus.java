package com.example.mealupp.model;

/**
 * Estados de un pedido. Con pago en línea el recorrido es PENDING_PAYMENT -> PENDING ->
 * IN_PREPARATION -> DELIVERED. Un pedido sin pagar puede vencer (EXPIRED) y uno sin entregar puede
 * cancelarse (CANCELLED).
 */
public enum OrderStatus {
  PENDING_PAYMENT,
  PENDING,
  IN_PREPARATION,
  DELIVERED,
  CANCELLED,
  EXPIRED;

  public boolean canTransitionTo(OrderStatus next) {
    return switch (this) {
      case PENDING_PAYMENT -> next == PENDING || next == CANCELLED || next == EXPIRED;
      case PENDING -> next == IN_PREPARATION || next == CANCELLED;
      case IN_PREPARATION -> next == DELIVERED;
      case DELIVERED, CANCELLED, EXPIRED -> false;
    };
  }

  public boolean isFinal() {
    return this == DELIVERED || this == CANCELLED || this == EXPIRED;
  }

  /** Indica si el pedido cuenta en el total de almuerzos a preparar del día. */
  public boolean countsForTotals() {
    return this != PENDING_PAYMENT && this != CANCELLED && this != EXPIRED;
  }
}
