package com.example.mealupp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef";
  private static final Instant NOW = Instant.parse("2026-11-04T15:00:00Z");
  private static final long EIGHT_HOURS = 8 * 3600L;

  private final JwtService jwt = new JwtService(SECRET);

  private String token() {
    return jwt.generate("nataly", NOW, NOW.plusSeconds(EIGHT_HOURS));
  }

  @Test
  void aValidTokenReturnsItsData() {
    var claims = jwt.parse(token(), NOW.plusSeconds(60));

    assertThat(claims).isPresent();
    assertThat(claims.get().subject()).isEqualTo("nataly");
    assertThat(claims.get().issuedAt()).isEqualTo(NOW);
    assertThat(claims.get().expiresAt()).isEqualTo(NOW.plusSeconds(EIGHT_HOURS));
    assertThat(claims.get().id()).isNotBlank();
  }

  @Test
  void eachTokenHasItsOwnId() {
    assertThat(token()).isNotEqualTo(token());
  }

  @Test
  void worksOneMinuteBeforeExpiringAndNotAtTheMoment() {
    String token = token();

    assertThat(jwt.parse(token, NOW.plusSeconds(EIGHT_HOURS - 60))).isPresent();
    assertThat(jwt.parse(token, NOW.plusSeconds(EIGHT_HOURS))).isEmpty();
  }

  @Test
  void aChangedTokenIsRejected() {
    String[] parts = token().split("\\.");

    assertThat(jwt.parse(parts[0] + "." + parts[1] + "x." + parts[2], NOW)).isEmpty();
    assertThat(jwt.parse(parts[0] + "." + parts[1] + ".abc", NOW)).isEmpty();
  }

  @Test
  void aTokenSignedWithAnotherKeyIsRejected() {
    String other =
        new JwtService("ffffffffffffffffffffffffffffffff")
            .generate("nataly", NOW, NOW.plusSeconds(EIGHT_HOURS));

    assertThat(jwt.parse(other, NOW)).isEmpty();
  }

  @Test
  void garbageAndNullAreRejected() {
    assertThat(jwt.parse("inventado", NOW)).isEmpty();
    assertThat(jwt.parse("a.b.c", NOW)).isEmpty();
    assertThat(jwt.parse(null, NOW)).isEmpty();
  }

  @Test
  void theNoneAlgorithmIsRejected() {
    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    String payload = token().split("\\.")[1];

    assertThat(jwt.parse(header + "." + payload + ".", NOW)).isEmpty();
  }

  @Test
  void theKeyMustHaveAtLeast32Characters() {
    assertThatThrownBy(() -> new JwtService("corta")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new JwtService(null)).isInstanceOf(IllegalArgumentException.class);
  }
}
