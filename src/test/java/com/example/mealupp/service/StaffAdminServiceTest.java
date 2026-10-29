package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.StaffMemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pruebas unitarias de HU 6: asignación de roles. */
@ExtendWith(MockitoExtension.class)
class StaffAdminServiceTest {

  @Mock private StaffMemberRepository staffRepository;

  @InjectMocks private StaffAdminService service;

  /**
   * Tabla de decisión (criterio 1 y regla de la última administradora): rol actual x cantidad de
   * administradoras activas x rol nuevo.
   */
  @ParameterizedTest(name = "{0} con {1} admin(s) activa(s) -> {2}: permitido={3}")
  @CsvSource({
    "ADMIN,   1, KITCHEN, false",
    "ADMIN,   1, CASHIER, false",
    "ADMIN,   1, ADMIN,   true",
    "ADMIN,   2, KITCHEN, true",
    "KITCHEN, 1, CASHIER, true",
    "CASHIER, 1, ADMIN,   true"
  })
  void hu6_roleChangeDecisionTable(
      StaffRole current, long activeAdmins, StaffRole newRole, boolean allowed) {
    StaffMember member = new StaffMember("ana", "Ana Ruiz", "hash", current);
    when(staffRepository.findById(1L)).thenReturn(Optional.of(member));
    if (current == StaffRole.ADMIN) {
      when(staffRepository.countByRoleAndActiveTrue(StaffRole.ADMIN)).thenReturn(activeAdmins);
    }
    if (allowed) {
      when(staffRepository.save(any(StaffMember.class))).thenAnswer(inv -> inv.getArgument(0));
      assertThat(service.changeRole(1L, newRole).role()).isEqualTo(newRole);
      assertThat(member.getRole()).isEqualTo(newRole);
    } else {
      assertThatThrownBy(() -> service.changeRole(1L, newRole))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessage("Debe quedar al menos una persona con rol ADMIN");
      assertThat(member.getRole()).isEqualTo(current);
    }
  }

  @Test
  void hu6_unknownMemberReturnsNotFound() {
    when(staffRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.changeRole(99L, StaffRole.CASHIER))
        .isInstanceOf(NotFoundException.class);
  }
}
