package com.example.mealupp.integration.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Prueba el adaptador S3 contra un servidor HTTP falso que corre en la misma máquina. */
class S3ObjectStorageTest {

  private HttpServer server;
  private final List<String> requests = new ArrayList<>();
  private final Map<String, String> headers = new HashMap<>();
  private byte[] body;
  private int status = 200;
  private S3ObjectStorage storage;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath());
          body = exchange.getRequestBody().readAllBytes();
          exchange
              .getRequestHeaders()
              .forEach((name, values) -> headers.put(name.toLowerCase(), values.get(0)));
          exchange.sendResponseHeaders(status, -1);
          exchange.close();
        });
    server.start();
    Clock clock = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneOffset.UTC);
    storage =
        new S3ObjectStorage(
            "http://127.0.0.1:" + server.getAddress().getPort() + "/",
            "mealup",
            "auto",
            "AK",
            "SK",
            "https://cdn.mealupp.com/",
            clock);
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void putSendsTheFileToBucketSlashKeyWithAValidSignature() {
    byte[] data = {1, 2, 3};

    storage.put("dishes/1/abc_thumb.jpg", data, "image/jpeg");

    assertThat(requests).containsExactly("PUT /mealup/dishes/1/abc_thumb.jpg");
    assertThat(body).isEqualTo(data);
    TreeMap<String, String> signed = new TreeMap<>();
    signed.put("host", "127.0.0.1:" + server.getAddress().getPort());
    signed.put("x-amz-content-sha256", SigV4Signer.sha256Hex(data));
    signed.put("x-amz-date", "20261104T150000Z");
    signed.put("content-type", "image/jpeg");
    signed.put("cache-control", "public, max-age=31536000, immutable");
    String expected =
        new SigV4Signer("AK", "SK", "auto", "s3")
            .authorization(
                "PUT",
                "/mealup/dishes/1/abc_thumb.jpg",
                "",
                signed,
                SigV4Signer.sha256Hex(data),
                "20261104T150000Z");
    assertThat(headers.get("authorization")).isEqualTo(expected);
  }

  @Test
  void deleteSendsADeleteRequest() {
    status = 204;

    storage.delete("dishes/1/abc_thumb.jpg");

    assertThat(requests).containsExactly("DELETE /mealup/dishes/1/abc_thumb.jpg");
    assertThat(headers.get("authorization"))
        .startsWith("AWS4-HMAC-SHA256 Credential=AK/20261104/auto/s3/");
  }

  @Test
  void aFailedUploadIsReported() {
    status = 403;

    assertThatThrownBy(() -> storage.put("dishes/1/x.jpg", new byte[] {1}, "image/jpeg"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("HTTP 403");
  }

  @Test
  void thePublicUrlUsesTheCdnAddress() {
    assertThat(storage.publicUrl("dishes/1/x_thumb.jpg"))
        .isEqualTo("https://cdn.mealupp.com/dishes/1/x_thumb.jpg");
  }
}
