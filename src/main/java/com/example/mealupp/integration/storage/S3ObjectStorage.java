package com.example.mealupp.integration.storage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Adaptador para almacenamiento compatible con S3 (AWS S3, Cloudflare R2, Backblaze B2, MinIO).
 * Firma las peticiones con SigV4 y usa rutas del tipo endpoint/bucket/clave, sin depender del SDK
 * de ningún proveedor.
 */
public final class S3ObjectStorage implements ObjectStorage {

  private static final DateTimeFormatter AMZ_DATE =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
  private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";
  private static final String EMPTY_HASH = SigV4Signer.sha256Hex(new byte[0]);

  private final HttpClient http = HttpClient.newHttpClient();
  private final String endpoint;
  private final String bucket;
  private final String publicBaseUrl;
  private final SigV4Signer signer;
  private final Clock clock;

  public S3ObjectStorage(
      String endpoint,
      String bucket,
      String region,
      String accessKey,
      String secretKey,
      String publicBaseUrl,
      Clock clock) {
    this.endpoint = trimSlash(endpoint);
    this.bucket = bucket;
    this.publicBaseUrl = trimSlash(publicBaseUrl);
    this.signer = new SigV4Signer(accessKey, secretKey, region, "s3");
    this.clock = clock;
  }

  @Override
  public void put(String key, byte[] data, String contentType) {
    URI uri = objectUri(key);
    String payloadHash = SigV4Signer.sha256Hex(data);
    String amzDate = AMZ_DATE.format(clock.instant());
    TreeMap<String, String> headers = baseHeaders(uri, payloadHash, amzDate);
    headers.put("content-type", contentType);
    headers.put("cache-control", CACHE_CONTROL);
    HttpRequest.Builder request =
        HttpRequest.newBuilder(uri).PUT(HttpRequest.BodyPublishers.ofByteArray(data));
    send(request, "PUT", uri, headers, payloadHash, amzDate, "guardar");
  }

  @Override
  public void delete(String key) {
    URI uri = objectUri(key);
    String amzDate = AMZ_DATE.format(clock.instant());
    TreeMap<String, String> headers = baseHeaders(uri, EMPTY_HASH, amzDate);
    HttpRequest.Builder request = HttpRequest.newBuilder(uri).DELETE();
    send(request, "DELETE", uri, headers, EMPTY_HASH, amzDate, "borrar");
  }

  @Override
  public String publicUrl(String key) {
    return publicBaseUrl + "/" + key;
  }

  private URI objectUri(String key) {
    return URI.create(endpoint + SigV4Signer.encodePath("/" + bucket + "/" + key));
  }

  private static TreeMap<String, String> baseHeaders(URI uri, String payloadHash, String amzDate) {
    TreeMap<String, String> headers = new TreeMap<>();
    headers.put("host", uri.getAuthority());
    headers.put("x-amz-content-sha256", payloadHash);
    headers.put("x-amz-date", amzDate);
    return headers;
  }

  private void send(
      HttpRequest.Builder request,
      String method,
      URI uri,
      TreeMap<String, String> headers,
      String payloadHash,
      String amzDate,
      String action) {
    String authorization =
        signer.authorization(method, uri.getRawPath(), "", headers, payloadHash, amzDate);
    for (Map.Entry<String, String> header : headers.entrySet()) {
      if (!header.getKey().equals("host")) {
        request.header(header.getKey(), header.getValue());
      }
    }
    request.header("Authorization", authorization);
    try {
      HttpResponse<Void> response =
          http.send(request.build(), HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException(
            "No se pudo " + action + " el archivo (HTTP " + response.statusCode() + ")");
      }
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo " + action + " el archivo", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Se interrumpió al " + action + " el archivo", ex);
    }
  }

  private static String trimSlash(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
