package com.example.mealupp.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.mealupp.exception.PaymentGatewayException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Prueba el cliente y el adaptador contra un Mercado Pago de mentira (servidor local). */
class MercadoPagoApiTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-21T18:00:00Z"), ZoneId.of("America/Bogota"));

  private HttpServer server;
  private MercadoPagoClient client;
  private final AtomicReference<String> preferenceBody = new AtomicReference<>();
  private final AtomicReference<String> authorization = new AtomicReference<>();
  private final AtomicReference<String> idempotency = new AtomicReference<>();
  private volatile int preferenceStatus = 201;
  private volatile String preferenceResponse =
      "{\"id\":\"p1\",\"init_point\":\"https://mp.test/pay/p1\"}";

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/checkout/preferences",
        exchange -> {
          preferenceBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          idempotency.set(exchange.getRequestHeaders().getFirst("X-Idempotency-Key"));
          reply(exchange, preferenceStatus, preferenceResponse);
        });
    server.createContext(
        "/v1/payments/",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          if (path.endsWith("/777")) {
            reply(
                exchange,
                200,
                "{\"status\":\"approved\",\"external_reference\":\"MU-1-1\","
                    + "\"transaction_amount\":12000.0,\"payer\":{\"id\":1}}");
          } else if (path.endsWith("/500")) {
            reply(exchange, 500, "{}");
          } else if (path.endsWith("/list")) {
            reply(exchange, 200, "[1,2]");
          } else {
            reply(exchange, 404, "{}");
          }
        });
    server.start();
    client =
        new MercadoPagoClient(
            "http://127.0.0.1:" + server.getAddress().getPort() + "/", "TEST-token");
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private MercadoPagoPaymentGateway gateway(String notificationUrl, String frontendUrl) {
    return new MercadoPagoPaymentGateway(client, notificationUrl, frontendUrl, 15, CLOCK);
  }

  private static PaymentGateway.PaymentRequest request() {
    return new PaymentGateway.PaymentRequest(
        5L, "MU-5-9", new BigDecimal("24000"), "COP", "Pedido MealUpp #5");
  }

  @Test
  void createsThePreferenceAndReturnsTheCheckoutUrl() {
    PaymentGateway.PaymentSession session =
        gateway("https://x.ngrok.app/api/payments/mercadopago/webhook", "http://localhost:5173/")
            .createTransaction(request());

    assertThat(session.providerReference()).isEqualTo("MU-5-9");
    assertThat(session.paymentUrl()).isEqualTo("https://mp.test/pay/p1");
    assertThat(authorization.get()).isEqualTo("Bearer TEST-token");
    assertThat(idempotency.get()).isEqualTo("MU-5-9");
    assertThat(preferenceBody.get())
        .contains("\"external_reference\":\"MU-5-9\"")
        .contains("\"unit_price\":24000")
        .contains("\"currency_id\":\"COP\"")
        .contains("\"success\":\"http://localhost:5173/pedido/5\"")
        .contains("\"notification_url\":\"https://x.ngrok.app/api/payments/mercadopago/webhook\"")
        .contains("\"expires\":true")
        .contains("\"expiration_date_to\":\"2026-11-21T18:15:00.000+00:00\"")
        .doesNotContain("auto_return");
  }

  @Test
  void asksForAutoReturnOnlyWithHttpsAndOmitsAnEmptyNotificationUrl() {
    gateway("", "https://mealupp.example.com").createTransaction(request());

    assertThat(preferenceBody.get())
        .contains("\"auto_return\":\"approved\"")
        .doesNotContain("notification_url");
  }

  @Test
  void fallsBackToTheSandboxInitPoint() {
    preferenceResponse = "{\"id\":\"p1\",\"sandbox_init_point\":\"https://sb.test/p1\"}";

    assertThat(gateway(null, "http://localhost:5173").createTransaction(request()).paymentUrl())
        .isEqualTo("https://sb.test/p1");
  }

  @Test
  void failsWhenThereIsNoCheckoutUrl() {
    preferenceResponse = "{\"id\":\"p1\"}";

    assertThatThrownBy(() -> gateway(null, "http://localhost:5173").createTransaction(request()))
        .isInstanceOf(PaymentGatewayException.class);
  }

  @Test
  void failsWhenMercadoPagoRejectsThePreference() {
    preferenceStatus = 400;
    preferenceResponse = "{\"message\":\"invalid\"}";

    assertThatThrownBy(() -> gateway(null, "http://localhost:5173").createTransaction(request()))
        .isInstanceOf(PaymentGatewayException.class)
        .hasMessageContaining("400");
  }

  @Test
  void failsOnAnUnreadableAnswer() {
    preferenceResponse = "esto no es json";

    assertThatThrownBy(() -> gateway(null, "http://localhost:5173").createTransaction(request()))
        .isInstanceOf(PaymentGatewayException.class);
  }

  @Test
  void failsWhenTheAnswerIsNotAnObject() {
    preferenceResponse = "[1]";

    assertThatThrownBy(() -> gateway(null, "http://localhost:5173").createTransaction(request()))
        .isInstanceOf(PaymentGatewayException.class);
  }

  @Test
  void readsAPayment() {
    Optional<Map<String, Object>> payment = client.findPayment("777");

    assertThat(payment).isPresent();
    assertThat(payment.get()).containsEntry("status", "approved");
    assertThat(payment.get()).containsEntry("external_reference", "MU-1-1");
    assertThat(payment.get().get("transaction_amount")).isEqualTo(new BigDecimal("12000.0"));
  }

  @Test
  void returnsEmptyWhenThePaymentDoesNotExist() {
    assertThat(client.findPayment("1")).isEmpty();
  }

  @Test
  void failsWhenThePaymentLookupBreaks() {
    assertThatThrownBy(() -> client.findPayment("500")).isInstanceOf(PaymentGatewayException.class);
    assertThatThrownBy(() -> client.findPayment("list"))
        .isInstanceOf(PaymentGatewayException.class);
  }

  @Test
  void failsWhenMercadoPagoCannotBeReached() {
    MercadoPagoClient offline = new MercadoPagoClient("http://127.0.0.1:1", "TEST-token");

    assertThatThrownBy(() -> offline.findPayment("1")).isInstanceOf(PaymentGatewayException.class);
  }
}
