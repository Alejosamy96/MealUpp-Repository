package com.example.mealupp.service;

import com.example.mealupp.dto.LoginResponse;
import com.example.mealupp.exception.InvalidCredentialsException;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.repository.StaffMemberRepository;
import com.example.mealupp.security.StaffPrincipal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * HU-05: el personal registrado inicia sesión con usuario y contraseña y recibe un token que vence
 * después de mealup.staff.token-hours. Corrige DEF-01: ya no se usa HTTP Basic.
 */
@Service
public class StaffAuthService {

  static final String INVALID_CREDENTIALS = "Usuario o contraseña incorrectos";

  private final StaffMemberRepository staffRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;
  private final Duration tokenDuration;
  private final SecureRandom random = new SecureRandom();
  private final Map<String, Session> sessions = new ConcurrentHashMap<>();

  private record Session(String username, LocalDateTime expiresAt) {}

  public StaffAuthService(
      StaffMemberRepository staffRepository,
      PasswordEncoder passwordEncoder,
      Clock clock,
      @Value("${mealup.staff.token-hours}") long tokenHours) {
    this.staffRepository = staffRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
    this.tokenDuration = Duration.ofHours(tokenHours);
  }

  public LoginResponse login(String username, String password) {
    StaffMember member =
        staffRepository
            .findByUsername(username)
            .filter(StaffMember::isActive)
            .filter(m -> passwordEncoder.matches(password, m.getPasswordHash()))
            .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
    String token = newToken();
    LocalDateTime expiresAt = LocalDateTime.now(clock).plus(tokenDuration);
    sessions.put(token, new Session(member.getUsername(), expiresAt));
    return new LoginResponse(
        token, expiresAt, member.getFullName(), member.getRole(), member.getRole().sections());
  }

  /** El rol se lee de la base de datos en cada petición, así un cambio de rol aplica de una vez. */
  public Optional<StaffPrincipal> findPrincipalByToken(String token) {
    Session session = sessions.get(token);
    if (session == null) {
      return Optional.empty();
    }
    if (!LocalDateTime.now(clock).isBefore(session.expiresAt())) {
      sessions.remove(token);
      return Optional.empty();
    }
    return staffRepository
        .findByUsername(session.username())
        .filter(StaffMember::isActive)
        .map(m -> new StaffPrincipal(m.getUsername(), m.getRole()));
  }

  public void logout(String token) {
    sessions.remove(token);
  }

  private String newToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
