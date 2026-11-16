package com.example.mealupp.model;

/** Zonas donde Ferxxos entrega pedidos (alcance del proyecto). */
public enum DeliveryZone {
  SHOPPING_CENTER("Centro comercial"),
  FURNITURE_STREET("Calle de los Muebles");

  private final String label;

  DeliveryZone(String label) {
    this.label = label;
  }

  /** Texto que se muestra al cliente. */
  public String label() {
    return label;
  }
}
