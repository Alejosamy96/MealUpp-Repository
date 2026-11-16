package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.mealupp.dto.LoginResponse;
import com.example.mealupp.exception.InvalidCredentialsException;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.StaffMemberRepository;
import com.example.mealupp.security.JwtService;
import com.example.mealupp.security.StaffPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Inicio de sesión con token JWT: vencimiento, cierre de sesión y cuentas desactivadas. */
@ExtendWith(MockitoExtension.class)
class StaffAuthServiceTest {

  private static final String SECRET = "clave-de-pruebas-clave-de-pruebas-0123456789";

  /** Reloj que se puede adelantar para probar el vencimiento. */
  private static final class MovableClock extends Clock {
    private Instant now = Instant.parse("2026-11-04T14:00:00Z");

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  @Mock private StaffMemberRepository staffRepository;

  private final PasswordEncoder encoder =
      PasswordEncoderFactories.createDelegatingPasswordEncoder();
  private final MovableClock clock = new MovableClock();
  private StaffAuthService service;
  private StaffMember member;

  @BeforeEach
  void setUp() {
    service = new StaffAuthService(staffRepository, encoder, clock, new JwtService(SECRET), 8);
    member =
        new StaffMember("nataly", "Nataly Nieto", encoder.encode("clave-1234"), StaffRole.ADMIN);
  }

  private String loginToken() {
    when(staffRepository.findByUsername("nataly")).thenReturn(Optional.of(member));
    return service.login("nataly", "clave-1234").token();
  }

  @Test
  void loginReturnsATokenTheRoleAndTheSectionsOfTheRole() {
    when(staffRepository.findByUsername("nataly")).thenReturn(Optional.of(member));

    LoginResponse response = service.login("nataly", "clave-1234");

    assertThat(response.token().split("\\.")).hasSize(3);
    assertThat(response.fullName()).isEqualTo("Nataly Nieto");
    assertThat(response.role()).isEqualTo(StaffRole.ADMIN);
    assertThat(response.sections()).containsExactlyElementsOf(StaffRole.ADMIN.sections());
    assertThat(response.expiresAt().toString()).isEqualTo("2026-11-04T22:00");
  }

  @Test
  void aWrongPasswordAndAnUnknownUserGetTheSameError() {
    when(staffRepository.findByUsername("nataly")).thenReturn(Optional.of(member));
    when(staffRepository.findByUsername("nadie")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.login("nataly", "otra-clave"))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Usuario o contraseña incorrectos");
    assertThatThrownBy(() -> service.login("nadie", "clave-1234"))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Usuario o contraseña incorrectos");
  }

  @Test
  void anInactiveAccountCannotLogIn() {
    member.setActive(false);
    when(staffRepository.findByUsername("nataly")).thenReturn(Optional.of(member));

    assertThatThrownBy(() -> service.login("nataly", "clave-1234"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void theTokenIdentifiesThePersonWhileItIsValid() {
    String token = loginToken();

    assertThat(service.findPrincipalByToken(token))
        .contains(new StaffPrincipal("nataly", StaffRole.ADMIN));
  }

  @Test
  void theTokenWorksOneMinuteBeforeExpiringAndStopsAtEightHours() {
    String token = loginToken();

    clock.advance(Duration.ofHours(8).minusMinutes(1));
    assertThat(service.findPrincipalByToken(token)).isPresent();

    clock.advance(Duration.ofMinutes(1));
    assertThat(service.findPrincipalByToken(token)).isEmpty();
  }

  @Test
  void afterLogoutTheTokenIsNoLongerValid() {
    String token = loginToken();

    service.logout(token);

    assertThat(service.findPrincipalByToken(token)).isEmpty();
  }

  @Test
  void logoutOfOneTokenDoesNotAffectAnother() {
    String first = loginToken();
    String second = loginToken();

    service.logout(first);

    assertThat(service.findPrincipalByToken(second)).isPresent();
  }

  @Test
  void aDeactivatedPersonLosesAccessWithTheTokenTheyAlreadyHad() {
    String token = loginToken();
    member.setActive(false);

    assertThat(service.findPrincipalByToken(token)).isEmpty();
  }

  @Test
  void aRoleChangeAppliesRightAway() {
    String token = loginToken();
    member.setRole(StaffRole.KITCHEN);

    assertThat(service.findPrincipalByToken(token))
        .contains(new StaffPrincipal("nataly", StaffRole.KITCHEN));
  }

  @Test
  void invalidAndTamperedTokensAreRejected() {
    String token = loginToken();

    assertThat(service.findPrincipalByToken("inventado")).isEmpty();
    assertThat(service.findPrincipalByToken(token + "x")).isEmpty();
    assertThat(service.findPrincipalByToken(null)).isEmpty();
  }
}
