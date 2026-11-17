package com.example.mealupp.dto;

import java.time.LocalTime;
import java.util.List;

/** Reglas públicas que el frontend necesita para armar el formulario de pedido. */
public record PublicConfigResponse(
    String timeZone,
    LocalTime cutoffTime,
    LocalTime deliveryStart,
    LocalTime deliveryEnd,
    int minQuantity,
    int maxQuantity,
    int maxOrdersPerDay,
    boolean paymentsRequired,
    String currency,
    int paymentExpirationMinutes,
    List<Zone> zones) {

  /** Zona de entrega: "code" es el valor que se envía y "label" el texto que se muestra. */
  public record Zone(String code, String label) {}
}
