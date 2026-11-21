package com.example.mealupp.controller;

import com.example.mealupp.dto.WebhookAck;
import com.example.mealupp.service.MercadoPagoNotificationService;
import com.example.mealupp.util.MiniJson;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aviso (webhook) de Mercado Pago. Es público, pero solo se acepta si trae una firma x-signature
 * válida. Existe solo con mealup.payments.provider=mercadopago.
 */
@RestController
@RequestMapping("/api/payments/mercadopago")
@ConditionalOnProperty(name = "mealup.payments.provider", havingValue = "mercadopago")
public class MercadoPagoWebhookController {

  private final MercadoPagoNotificationService notifications;

  public MercadoPagoWebhookController(MercadoPagoNotificationService notifications) {
    this.notifications = notifications;
  }

  @PostMapping("/webhook")
  public WebhookAck webhook(
      @RequestParam(name = "data.id", required = false) String queryDataId,
      @RequestParam(name = "type", required = false) String queryType,
      @RequestHeader(name = "x-signature", required = false) String signature,
      @RequestHeader(name = "x-request-id", required = false) String requestId,
      @RequestBody(required = false) String body) {
    String dataId = queryDataId;
    String type = queryType;
    if ((dataId == null || type == null) && body != null && !body.isBlank()) {
      Map<?, ?> json = tryParse(body);
      if (json != null) {
        if (type == null && json.get("type") instanceof String text) {
          type = text;
        }
        if (dataId == null
            && json.get("data") instanceof Map<?, ?> data
            && data.get("id") != null) {
          dataId = data.get("id").toString();
        }
      }
    }
    notifications.handle(dataId, type, signature, requestId);
    return new WebhookAck(true);
  }

  private static Map<?, ?> tryParse(String body) {
    try {
      return MiniJson.parseTree(body) instanceof Map<?, ?> map ? map : null;
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
