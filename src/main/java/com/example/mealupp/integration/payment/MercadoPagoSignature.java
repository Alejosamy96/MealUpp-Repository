package com.example.mealupp.integration.payment;

import com.example.mealupp.util.HmacSigner;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Valida la firma de los avisos (webhooks) de Mercado Pago. El encabezado x-signature trae
 * "ts=...,v1=..."; v1 es el HMAC-SHA256 en hexadecimal de "id:[data.id];request-id:[x-request-id];
 * ts:[ts];" con la clave secreta del webhook. Si falta algún dato, esa parte se omite del texto.
 */
public final class MercadoPagoSignature {

  private MercadoPagoSignature() {}

  public static boolean isValid(
      String secret, String signatureHeader, String requestId, String dataId) {
    if (secret == null || secret.isBlank() || signatureHeader == null) {
      return false;
    }
    String ts = null;
    String v1 = null;
    for (String part : signatureHeader.split(",")) {
      String[] pair = part.trim().split("=", 2);
      if (pair.length != 2) {
        continue;
      }
      if ("ts".equals(pair[0].trim())) {
        ts = pair[1].trim();
      } else if ("v1".equals(pair[0].trim())) {
        v1 = pair[1].trim();
      }
    }
    if (ts == null || v1 == null) {
      return false;
    }
    String expected = HmacSigner.hmacHex(secret, manifest(dataId, requestId, ts));
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
  }

  static String manifest(String dataId, String requestId, String ts) {
    StringBuilder sb = new StringBuilder();
    if (dataId != null && !dataId.isBlank()) {
      sb.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
    }
    if (requestId != null && !requestId.isBlank()) {
      sb.append("request-id:").append(requestId).append(';');
    }
    sb.append("ts:").append(ts).append(';');
    return sb.toString();
  }
}
