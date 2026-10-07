package com.example.mealupp.config;

import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglas de pedidos que se configuran en application.properties (no van fijas en el código).
 *
 * @param cutoffTime hora límite para hacer pedidos del día (se acepta antes de esta hora)
 * @param deliveryStart primera hora de entrega permitida (inclusive)
 * @param deliveryEnd última hora de entrega permitida (inclusive)
 * @param minQuantity cantidad mínima de almuerzos por pedido
 * @param maxQuantity cantidad máxima de almuerzos por pedido
 */
@ConfigurationProperties(prefix = "mealup.orders")
public record OrderRulesProperties(
    LocalTime cutoffTime,
    LocalTime deliveryStart,
    LocalTime deliveryEnd,
    int minQuantity,
    int maxQuantity) {}
