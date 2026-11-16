package com.example.mealupp.security;

import com.example.mealupp.util.HmacSigner;
import com.example.mealupp.util.MiniJson;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tokens JWT firmados con HS256. No guardan sesión en el servidor: la firma y la fecha de
 * vencimiento bastan para validarlos, así que la aplicación puede correr en varias instancias.
 */
public final class JwtService {

  /** Datos que viajan dentro del token. */
  public record Claims(String subject, String id, Instant issuedAt, Instant expiresAt) {}

  private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

  private final byte[] key;
  private final String encodedHeader;
  private final SecureRandom random = new SecureRandom();

  public JwtService(String secret) {
    if (secret == null || secret.length() < 32) {
      throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 caracteres");
    }
    this.key = secret.getBytes(StandardCharsets.UTF_8);
    this.encodedHeader = ENCODER.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
  }

  public String generate(String subject, Instant issuedAt, Instant expiresAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sub", subject);
    payload.put("jti", newId());
    payload.put("iat", issuedAt.getEpochSecond());
    payload.put("exp", expiresAt.getEpochSecond());
    String body = ENCODER.encodeToString(MiniJson.write(payload).getBytes(StandardCharsets.UTF_8));
    String signingInput = encodedHeader + "." + body;
    String signature =
        ENCODER.encodeToString(HmacSigner.hmac(key, signingInput.getBytes(StandardCharsets.UTF_8)));
    return signingInput + "." + signature;
  }

  /** Devuelve los datos del token si la firma es válida y no ha vencido; si no, vacío. */
  public Optional<Claims> parse(String token, Instant now) {
    if (token == null) {
      return Optional.empty();
    }
    try {
      String[] parts = token.split("\\.", -1);
      if (parts.length != 3 || !parts[0].equals(encodedHeader)) {
        return Optional.empty();
      }
      byte[] expected =
          HmacSigner.hmac(key, (parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
      byte[] given = Base64.getUrlDecoder().decode(parts[2]);
      if (!MessageDigest.isEqual(expected, given)) {
        return Optional.empty();
      }
      Map<String, String> claims =
          MiniJson.parseFlat(
              new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
      long expiresAt = Long.parseLong(claims.get("exp"));
      if (expiresAt <= now.getEpochSecond()) {
        return Optional.empty();
      }
      return Optional.of(
          new Claims(
              claims.get("sub"),
              claims.get("jti"),
              Instant.ofEpochSecond(Long.parseLong(claims.get("iat"))),
              Instant.ofEpochSecond(expiresAt)));
    } catch (RuntimeException ex) {
      return Optional.empty();
    }
  }

  private String newId() {
    byte[] bytes = new byte[16];
    random.nextBytes(bytes);
    return ENCODER.encodeToString(bytes);
  }
}
