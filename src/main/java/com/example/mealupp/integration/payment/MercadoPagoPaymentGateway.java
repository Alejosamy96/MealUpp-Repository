package com.example.mealupp.integration.payment;

import com.example.mealupp.exception.PaymentGatewayException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de Mercado Pago (Checkout Pro). Crea una preferencia de pago y devuelve su dirección:
 * el cliente paga en la página de Mercado Pago y vuelve al frontend. El resultado llega después,
 * por el webhook. La referencia propia del pedido viaja como external_reference.
 */
public final class MercadoPagoPaymentGateway implements PaymentGateway {

  private static final DateTimeFormatter EXPIRATION =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");

  private final MercadoPagoClient client;
  private final String notificationUrl;
  private final String frontendUrl;
  private final int expirationMinutes;
  private final Clock clock;

  public MercadoPagoPaymentGateway(
      MercadoPagoClient client,
      String notificationUrl,
      String frontendUrl,
      int expirationMinutes,
      Clock clock) {
    this.client = client;
    this.notificationUrl = notificationUrl;
    this.frontendUrl =
        frontendUrl.endsWith("/")
            ? frontendUrl.substring(0, frontendUrl.length() - 1)
            : frontendUrl;
    this.expirationMinutes = expirationMinutes;
    this.clock = clock;
  }

  @Override
  public PaymentSession createTransaction(PaymentRequest request) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", String.valueOf(request.orderId()));
    item.put("title", request.description());
    item.put("quantity", 1);
    item.put("unit_price", request.amount());
    item.put("currency_id", request.currency());

    String back = frontendUrl + "/pedido/" + request.orderId();
    Map<String, Object> backUrls = new LinkedHashMap<>();
    backUrls.put("success", back);
    backUrls.put("failure", back);
    backUrls.put("pending", back);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("items", List.of(item));
    body.put("external_reference", request.reference());
    body.put("back_urls", backUrls);
    // Mercado Pago solo acepta el regreso automático si las direcciones son HTTPS públicas.
    if (frontendUrl.startsWith("https://")) {
      body.put("auto_return", "approved");
    }
    if (notificationUrl != null && !notificationUrl.isBlank()) {
      body.put("notification_url", notificationUrl);
    }
    body.put("expires", true);
    OffsetDateTime expiresAt =
        OffsetDateTime.now(clock)
            .withOffsetSameInstant(ZoneOffset.UTC)
            .plusMinutes(expirationMinutes);
    body.put("expiration_date_to", EXPIRATION.format(expiresAt));

    Map<String, Object> response = client.createPreference(body, request.reference());
    String paymentUrl = text(response.get("init_point"));
    if (paymentUrl == null) {
      paymentUrl = text(response.get("sandbox_init_point"));
    }
    if (paymentUrl == null) {
      throw new PaymentGatewayException("Mercado Pago no devolvió la dirección de pago");
    }
    return new PaymentSession(request.reference(), paymentUrl);
  }

  private static String text(Object value) {
    return value instanceof String text && !text.isBlank() ? text : null;
  }
}
