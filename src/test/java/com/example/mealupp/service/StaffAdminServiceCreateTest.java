package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mealupp.dto.CreateStaffMemberRequest;
import com.example.mealupp.dto.StaffMemberResponse;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.StaffMemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Registro de personal y activación de cuentas. El cambio de rol está en StaffAdminServiceTest. */
@ExtendWith(MockitoExtension.class)
class StaffAdminServiceCreateTest {

  @Mock private StaffMemberRepository staffRepository;

  private final PasswordEncoder encoder =
      PasswordEncoderFactories.createDelegatingPasswordEncoder();
  private StaffAdminService service;

  @BeforeEach
  void setUp() {
    service = new StaffAdminService(staffRepository, encoder);
  }

  private static CreateStaffMemberRequest request(String username, String password) {
    return new CreateStaffMemberRequest(username, " Carlos Pérez ", password, StaffRole.KITCHEN);
  }

  @Test
  void createSavesThePersonWithAHashedPasswordAndALowerCaseUsername() {
    when(staffRepository.existsByUsername("carlos.perez")).thenReturn(false);
    when(staffRepository.save(any(StaffMember.class))).thenAnswer(inv -> inv.getArgument(0));

    StaffMemberResponse response = service.create(request("  Carlos.Perez ", "clave-1234"));

    assertThat(response.username()).isEqualTo("carlos.perez");
    assertThat(response.fullName()).isEqualTo("Carlos Pérez");
    assertThat(response.role()).isEqualTo(StaffRole.KITCHEN);
    ArgumentCaptor<StaffMember> saved = ArgumentCaptor.forClass(StaffMember.class);
    verify(staffRepository).save(saved.capture());
    assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("clave-1234");
    assertThat(encoder.matches("clave-1234", saved.getValue().getPasswordHash())).isTrue();
  }

  @Test
  void createRejectsAUsernameThatIsAlreadyTaken() {
    when(staffRepository.existsByUsername("carlos")).thenReturn(true);
    CreateStaffMemberRequest request = request("carlos", "clave-1234");

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Ese usuario ya está registrado");
    verify(staffRepository, never()).save(any());
  }

  @Test
  void usernameLengthBoundaries() {
    when(staffRepository.save(any(StaffMember.class))).thenAnswer(inv -> inv.getArgument(0));

    assertThat(service.create(request("abc", "clave-1234")).username()).isEqualTo("abc");
    assertThat(service.create(request("a".repeat(30), "clave-1234")).username()).hasSize(30);
    assertThatThrownBy(() -> service.create(request("ab", "clave-1234")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("entre 3 y 30");
    assertThatThrownBy(() -> service.create(request("a".repeat(31), "clave-1234")))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void usernameCanOnlyHaveSimpleCharacters() {
    assertThatThrownBy(() -> service.create(request("carlos perez", "clave-1234")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("solo puede tener");
    assertThatThrownBy(() -> service.create(request("carlos@x", "clave-1234")))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void passwordBoundaryIsEightCharacters() {
    when(staffRepository.save(any(StaffMember.class))).thenAnswer(inv -> inv.getArgument(0));

    assertThat(service.create(request("carlos", "12345678"))).isNotNull();
    assertThatThrownBy(() -> service.create(request("carlos", "1234567")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("al menos 8");
  }

  @Test
  void deactivatingAnOrdinaryPersonWorks() {
    StaffMember cook = new StaffMember("cocina1", "Carlos", "hash", StaffRole.KITCHEN);
    when(staffRepository.findById(2L)).thenReturn(Optional.of(cook));
    when(staffRepository.save(any(StaffMember.class))).thenAnswer(inv -> inv.getArgument(0));

    assertThat(service.setActive(2L, false).active()).isFalse();
    assertThat(service.setActive(2L, true).active()).isTrue();
  }

  @Test
  void theLastActiveAdminCannotBeDeactivated() {
    StaffMember admin = new StaffMember("staff", "Admin", "hash", StaffRole.ADMIN);
    when(staffRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(staffRepository.countByRoleAndActiveTrue(StaffRole.ADMIN)).thenReturn(1L);

    assertThatThrownBy(() -> service.setActive(1L, false))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Debe quedar al menos una persona con rol ADMIN");
    assertThat(admin.isActive()).isTrue();
  }

  @Test
  void anAdminCanBeDeactivatedWhenThereIsAnotherOne() {
    StaffMember admin = new StaffMember("staff", "Admin", "hash", StaffRole.ADMIN);
    when(staffRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(staffRepository.countByRoleAndActiveTrue(StaffRole.ADMIN)).thenReturn(2L);
    when(staffRepository.save(any(StaffMember.class))).thenAnswer(inv -> inv.getArgument(0));

    assertThat(service.setActive(1L, false).active()).isFalse();
  }

  @Test
  void setActiveOfAnUnknownPersonIsNotFound() {
    when(staffRepository.findById(9L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.setActive(9L, false)).isInstanceOf(NotFoundException.class);
  }
}
