package com.example.mealupp.controller;

import com.example.mealupp.dto.WebhookAck;
import com.example.mealupp.service.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aviso (webhook) de la pasarela de pagos. Es público, pero solo se acepta si trae la firma
 * HMAC-SHA256 del cuerpo en el encabezado X-Signature.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/webhook")
  public WebhookAck webhook(
      @RequestBody String rawBody,
      @RequestHeader(name = "X-Signature", required = false) String signature) {
    paymentService.handleWebhook(rawBody, signature == null ? "" : signature);
    return new WebhookAck(true);
  }
}
