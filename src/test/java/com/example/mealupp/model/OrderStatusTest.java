package com.example.mealupp.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
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
    "IN_PREPARATION, READY,          true",
    "IN_PREPARATION, DELIVERED,      false",
    "READY,          DELIVERED,      true",
    "READY,          IN_PREPARATION, false",
    "READY,          CANCELLED,      false",
    "PENDING,        READY,          false",
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

  /** Estados nuevos del pago en línea. */
  @ParameterizedTest(name = "{0} -> {1} permitido={2}")
  @CsvSource({
    "PENDING_PAYMENT, PENDING,          true",
    "PENDING_PAYMENT, EXPIRED,          true",
    "PENDING_PAYMENT, CANCELLED,        true",
    "PENDING_PAYMENT, IN_PREPARATION,   false",
    "PENDING_PAYMENT, DELIVERED,        false",
    "PENDING,         PENDING_PAYMENT,  false",
    "PENDING,         EXPIRED,          false",
    "IN_PREPARATION,  EXPIRED,          false",
    "EXPIRED,         PENDING,          false",
    "EXPIRED,         PENDING_PAYMENT,  false",
    "EXPIRED,         CANCELLED,        false"
  })
  void paymentStatesTransitionTable(OrderStatus current, OrderStatus next, boolean expected) {
    assertThat(current.canTransitionTo(next)).isEqualTo(expected);
  }

  @Test
  void finalStatesAreDeliveredCancelledAndExpired() {
    assertThat(OrderStatus.values())
        .filteredOn(OrderStatus::isFinal)
        .containsExactlyInAnyOrder(
            OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.EXPIRED);
  }

  @Test
  void onlyConfirmedOrdersCountForTheDailyTotal() {
    assertThat(OrderStatus.values())
        .filteredOn(OrderStatus::countsForTotals)
        .containsExactlyInAnyOrder(
            OrderStatus.PENDING,
            OrderStatus.IN_PREPARATION,
            OrderStatus.READY,
            OrderStatus.DELIVERED);
  }
}
