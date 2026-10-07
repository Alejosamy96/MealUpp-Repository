package com.example.mealupp.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** CP-15 (HU-04). Técnica: tabla de decisión estado actual x estado nuevo. */
class OrderStatusTest {

  @ParameterizedTest(name = "{0} -> {1} permitido={2}")
  @CsvSource({
    "PENDING,        IN_PREPARATION, true",
    "PENDING,        CANCELLED,      true",
    "PENDING,        DELIVERED,      false",
    "PENDING,        PENDING,        false",
    "IN_PREPARATION, DELIVERED,      true",
    "IN_PREPARATION, PENDING,        false",
    "IN_PREPARATION, CANCELLED,      false",
    "IN_PREPARATION, IN_PREPARATION, false",
    "DELIVERED,      PENDING,        false",
    "DELIVERED,      IN_PREPARATION, false",
    "DELIVERED,      CANCELLED,      false",
    "CANCELLED,      PENDING,        false",
    "CANCELLED,      IN_PREPARATION, false",
    "CANCELLED,      DELIVERED,      false"
  })
  void cp15_transitionTable(OrderStatus current, OrderStatus next, boolean expected) {
    assertThat(current.canTransitionTo(next)).isEqualTo(expected);
  }
}
