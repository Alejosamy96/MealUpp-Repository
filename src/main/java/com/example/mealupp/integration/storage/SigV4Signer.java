package com.example.mealupp.integration.storage;

import com.example.mealupp.util.HmacSigner;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

/** Firma de peticiones AWS Signature Version 4, la que usan S3 y los servicios compatibles. */
public final class SigV4Signer {

  private static final String ALGORITHM = "AWS4-HMAC-SHA256";

  private final String accessKey;
  private final String secretKey;
  private final String region;
  private final String service;

  public SigV4Signer(String accessKey, String secretKey, String region, String service) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.region = region;
    this.service = service;
  }

  /**
   * Calcula el valor del encabezado Authorization.
   *
   * @param headers encabezados a firmar, con el nombre en minúsculas y ordenados
   * @param amzDate fecha en formato yyyyMMdd'T'HHmmss'Z'
   */
  public String authorization(
      String method,
      String canonicalUri,
      String canonicalQuery,
      SortedMap<String, String> headers,
      String payloadHash,
      String amzDate) {
    String signedHeaders = String.join(";", headers.keySet());
    String canonicalHeaders =
        headers.entrySet().stream()
            .map((Map.Entry<String, String> e) -> e.getKey() + ":" + e.getValue().trim() + "\n")
            .collect(Collectors.joining());
    String canonicalRequest =
        method
            + "\n"
            + canonicalUri
            + "\n"
            + canonicalQuery
            + "\n"
            + canonicalHeaders
            + "\n"
            + signedHeaders
            + "\n"
            + payloadHash;
    String date = amzDate.substring(0, 8);
    String scope = date + "/" + region + "/" + service + "/aws4_request";
    String stringToSign =
        ALGORITHM
            + "\n"
            + amzDate
            + "\n"
            + scope
            + "\n"
            + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
    byte[] signingKey = signingKey(date);
    String signature =
        HmacSigner.hex(HmacSigner.hmac(signingKey, stringToSign.getBytes(StandardCharsets.UTF_8)));
    return ALGORITHM
        + " Credential="
        + accessKey
        + "/"
        + scope
        + ", SignedHeaders="
        + signedHeaders
        + ", Signature="
        + signature;
  }

  private byte[] signingKey(String date) {
    byte[] dateKey =
        HmacSigner.hmac(
            ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8),
            date.getBytes(StandardCharsets.UTF_8));
    byte[] regionKey = HmacSigner.hmac(dateKey, region.getBytes(StandardCharsets.UTF_8));
    byte[] serviceKey = HmacSigner.hmac(regionKey, service.getBytes(StandardCharsets.UTF_8));
    return HmacSigner.hmac(serviceKey, "aws4_request".getBytes(StandardCharsets.UTF_8));
  }

  public static String sha256Hex(byte[] data) {
    try {
      return HmacSigner.hex(MessageDigest.getInstance("SHA-256").digest(data));
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("SHA-256 no disponible", ex);
    }
  }

  /** Codifica cada tramo de la ruta según RFC 3986, como exige la firma. */
  public static String encodePath(String path) {
    StringBuilder sb = new StringBuilder();
    for (byte b : path.getBytes(StandardCharsets.UTF_8)) {
      char c = (char) (b & 0xFF);
      boolean unreserved =
          (c >= 'A' && c <= 'Z')
              || (c >= 'a' && c <= 'z')
              || (c >= '0' && c <= '9')
              || c == '-'
              || c == '_'
              || c == '.'
              || c == '~'
              || c == '/';
      if (unreserved) {
        sb.append(c);
      } else {
        sb.append(String.format("%%%02X", b & 0xFF));
      }
    }
    return sb.toString();
  }
}
