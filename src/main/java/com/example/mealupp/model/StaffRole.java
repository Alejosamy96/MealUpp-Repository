package com.example.mealupp.model;

import java.util.List;

/** Roles del personal del restaurante (HU-06). */
public enum StaffRole {
  /** Administradora: asigna roles y ve toda la información. */
  ADMIN(List.of("pedidos", "cocina", "caja", "personal")),
  /** Cocina: ve los pedidos que hay que preparar. */
  KITCHEN(List.of("cocina")),
  /** Caja: ve los pedidos con su valor. */
  CASHIER(List.of("caja"));

  private final List<String> sections;

  StaffRole(List<String> sections) {
    this.sections = sections;
  }

  /** Opciones del menú interno que le corresponden a este rol (criterio 2 de la HU-06). */
  public List<String> sections() {
    return sections;
  }
}
