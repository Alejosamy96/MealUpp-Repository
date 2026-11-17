package com.example.mealupp.service;

import com.example.mealupp.dto.LoginResponse;
import com.example.mealupp.exception.InvalidCredentialsException;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.repository.StaffMemberRepository;
import com.example.mealupp.security.JwtService;
import com.example.mealupp.security.StaffPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Inicio de sesión del personal con usuario y contraseña. Devuelve un token JWT firmado que vence
 * después de mealup.staff.token-hours. El servidor no guarda sesiones; solo recuerda, hasta que
 * venzan, los tokens de quienes cerraron sesión.
 */
@Service
public class StaffAuthService {

  static final String INVALID_CREDENTIALS = "Usuario o contraseña incorrectos";

  private final StaffMemberRepository staffRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;
  private final JwtService jwtService;
  private final Duration tokenDuration;
  private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

  public StaffAuthService(
      StaffMemberRepository staffRepository,
      PasswordEncoder passwordEncoder,
      Clock clock,
      JwtService jwtService,
      @Value("${mealup.staff.token-hours}") long tokenHours) {
    this.staffRepository = staffRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
    this.jwtService = jwtService;
    this.tokenDuration = Duration.ofHours(tokenHours);
  }

  public LoginResponse login(String username, String password) {
    StaffMember member =
        staffRepository
            .findByUsername(username)
            .filter(StaffMember::isActive)
            .filter(m -> passwordEncoder.matches(password, m.getPasswordHash()))
            .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
    Instant now = clock.instant();
    Instant expiresAt = now.plus(tokenDuration);
    String token = jwtService.generate(member.getUsername(), now, expiresAt);
    return new LoginResponse(
        token,
        LocalDateTime.ofInstant(expiresAt, clock.getZone()),
        member.getFullName(),
        member.getRole(),
        member.getRole().sections());
  }

  /** El rol se lee de la base de datos en cada petición, así un cambio de rol aplica de una vez. */
  public Optional<StaffPrincipal> findPrincipalByToken(String token) {
    return jwtService
        .parse(token, clock.instant())
        .filter(claims -> !revoked.containsKey(claims.id()))
        .flatMap(claims -> staffRepository.findByUsername(claims.subject()))
        .filter(StaffMember::isActive)
        .map(member -> new StaffPrincipal(member.getUsername(), member.getRole()));
  }

  public void logout(String token) {
    Instant now = clock.instant();
    jwtService.parse(token, now).ifPresent(claims -> revoked.put(claims.id(), claims.expiresAt()));
    revoked.values().removeIf(expiresAt -> !expiresAt.isAfter(now));
  }
}
