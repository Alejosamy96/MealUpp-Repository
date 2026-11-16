package com.example.mealupp.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** HU-06, criterio 2: cada rol tiene solo las opciones de menú que le corresponden. */
class StaffRoleTest {

  @Test
  void kitchenOnlyHasTheKitchenOption() {
    assertThat(StaffRole.KITCHEN.sections()).containsExactly("cocina");
  }

  @Test
  void cashierOnlyHasTheCashierOption() {
    assertThat(StaffRole.CASHIER.sections()).containsExactly("caja");
  }

  @Test
  void adminHasEveryOption() {
    assertThat(StaffRole.ADMIN.sections())
        .containsExactlyInAnyOrder("pedidos", "cocina", "caja", "entrega", "menu", "personal");
  }

  @Test
  void deliveryOnlyHasTheDeliveryOption() {
    assertThat(StaffRole.DELIVERY.sections()).containsExactly("entrega");
  }

  /** Tabla de decisión: rol x cambio de estado. */
  @ParameterizedTest(name = "{0}: {1} -> {2} permitido={3}")
  @CsvSource({
    "ADMIN,    PENDING_PAYMENT, PENDING,        true",
    "ADMIN,    PENDING,         IN_PREPARATION, true",
    "ADMIN,    IN_PREPARATION,  DELIVERED,      true",
    "KITCHEN,  PENDING,         IN_PREPARATION, true",
    "KITCHEN,  PENDING,         CANCELLED,      false",
    "KITCHEN,  IN_PREPARATION,  DELIVERED,      false",
    "CASHIER,  PENDING,         CANCELLED,      true",
    "CASHIER,  PENDING_PAYMENT, CANCELLED,      true",
    "CASHIER,  PENDING,         IN_PREPARATION, false",
    "CASHIER,  IN_PREPARATION,  CANCELLED,      false",
    "DELIVERY, IN_PREPARATION,  DELIVERED,      true",
    "DELIVERY, PENDING,         IN_PREPARATION, false",
    "DELIVERY, PENDING,         CANCELLED,      false"
  })
  void canMoveDecisionTable(StaffRole role, OrderStatus from, OrderStatus to, boolean expected) {
    assertThat(role.canMove(from, to)).isEqualTo(expected);
  }
}
