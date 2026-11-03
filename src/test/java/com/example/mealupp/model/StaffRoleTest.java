package com.example.mealupp.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
        .containsExactlyInAnyOrder("pedidos", "cocina", "caja", "personal");
  }
}
