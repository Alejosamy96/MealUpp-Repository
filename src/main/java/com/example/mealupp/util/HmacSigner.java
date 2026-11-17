package com.example.mealupp.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Firmas HMAC-SHA256: se usan para el token (JWT), el webhook de pagos y las peticiones a S3. */
public final class HmacSigner {

  private static final String ALGORITHM = "HmacSHA256";

  private HmacSigner() {}

  public static byte[] hmac(byte[] key, byte[] data) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(key, ALGORITHM));
      return mac.doFinal(data);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("No se pudo calcular la firma HMAC", ex);
    }
  }

  public static String hmacHex(String secret, String data) {
    return hex(
        hmac(secret.getBytes(StandardCharsets.UTF_8), data.getBytes(StandardCharsets.UTF_8)));
  }

  /** Compara en tiempo constante. Acepta la firma con o sin el prefijo "sha256=". */
  public static boolean matches(String secret, String data, String signatureHex) {
    if (secret == null || secret.isEmpty() || data == null || signatureHex == null) {
      return false;
    }
    String given = signatureHex.trim().toLowerCase(Locale.ROOT);
    if (given.startsWith("sha256=")) {
      given = given.substring("sha256=".length());
    }
    String expected = hmacHex(secret, data);
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
  }

  public static String hex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
