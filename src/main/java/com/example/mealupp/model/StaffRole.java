package com.example.mealupp.model;

import java.util.List;

/** Roles del personal del restaurante. */
public enum StaffRole {
  /** Administradora: asigna roles, administra el menú y ve toda la información. */
  ADMIN(List.of("pedidos", "cocina", "caja", "entrega", "menu", "personal")),
  /** Cocina: ve los pedidos que hay que preparar. */
  KITCHEN(List.of("cocina")),
  /** Caja: ve los pedidos con su valor. */
  CASHIER(List.of("caja")),
  /** Entrega: lleva los pedidos preparados al centro comercial y la Calle de los Muebles. */
  DELIVERY(List.of("entrega"));

  private final List<String> sections;

  StaffRole(List<String> sections) {
    this.sections = sections;
  }

  /** Opciones del menú interno que le corresponden a este rol. */
  public List<String> sections() {
    return sections;
  }

  /** Indica si este rol puede hacer el cambio de estado. No revisa si el cambio es válido. */
  public boolean canMove(OrderStatus from, OrderStatus to) {
    return switch (this) {
      case ADMIN -> true;
      case KITCHEN ->
          (from == OrderStatus.PENDING && to == OrderStatus.IN_PREPARATION)
              || (from == OrderStatus.IN_PREPARATION && to == OrderStatus.READY);
      case CASHIER ->
          (from == OrderStatus.PENDING || from == OrderStatus.PENDING_PAYMENT)
              && to == OrderStatus.CANCELLED;
      case DELIVERY -> from == OrderStatus.READY && to == OrderStatus.DELIVERED;
    };
  }
}
