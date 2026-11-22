package com.example.mealupp.integration.payment;

import com.example.mealupp.exception.PaymentGatewayException;
import com.example.mealupp.util.MiniJson;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** Llamadas HTTP a la API de Mercado Pago: crear la preferencia de pago y consultar un pago. */
public class MercadoPagoClient {

  private static final Duration TIMEOUT = Duration.ofSeconds(15);

  private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
  private final String apiUrl;
  private final String accessToken;

  public MercadoPagoClient(String apiUrl, String accessToken) {
    this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
    this.accessToken = accessToken;
  }

  /** Crea la preferencia de Checkout Pro. Devuelve el JSON de respuesta ya leído. */
  @SuppressWarnings("unchecked")
  public Map<String, Object> createPreference(Map<String, Object> body, String idempotencyKey) {
    HttpRequest request =
        base("/checkout/preferences")
            .header("Content-Type", "application/json")
            .header("X-Idempotency-Key", idempotencyKey)
            .POST(HttpRequest.BodyPublishers.ofString(MiniJson.write(body)))
            .build();
    HttpResponse<String> response = send(request);
    if (response.statusCode() / 100 != 2) {
      throw new PaymentGatewayException(
          "Mercado Pago rechazó la preferencia ("
              + response.statusCode()
              + "): "
              + response.body());
    }
    Object parsed = parse(response.body());
    if (parsed instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    throw new PaymentGatewayException("Respuesta inesperada de Mercado Pago");
  }

  /** Consulta un pago por su id. Vacío si Mercado Pago dice que no existe. */
  @SuppressWarnings("unchecked")
  public Optional<Map<String, Object>> findPayment(String paymentId) {
    String encoded = URLEncoder.encode(paymentId, StandardCharsets.UTF_8);
    HttpResponse<String> response = send(base("/v1/payments/" + encoded).GET().build());
    if (response.statusCode() == 404) {
      return Optional.empty();
    }
    if (response.statusCode() / 100 != 2) {
      throw new PaymentGatewayException(
          "No se pudo consultar el pago en Mercado Pago (" + response.statusCode() + ")");
    }
    Object parsed = parse(response.body());
    if (parsed instanceof Map<?, ?> map) {
      return Optional.of((Map<String, Object>) map);
    }
    throw new PaymentGatewayException("Respuesta inesperada de Mercado Pago");
  }

  private HttpRequest.Builder base(String path) {
    return HttpRequest.newBuilder(URI.create(apiUrl + path))
        .timeout(TIMEOUT)
        .header("Authorization", "Bearer " + accessToken)
        .header("Accept", "application/json");
  }

  private HttpResponse<String> send(HttpRequest request) {
    try {
      return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      throw new PaymentGatewayException("No se pudo hablar con Mercado Pago", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new PaymentGatewayException("La llamada a Mercado Pago fue interrumpida", ex);
    }
  }

  private static Object parse(String json) {
    try {
      return MiniJson.parseTree(json);
    } catch (IllegalArgumentException ex) {
      throw new PaymentGatewayException("Mercado Pago devolvió una respuesta ilegible", ex);
    }
  }
}
