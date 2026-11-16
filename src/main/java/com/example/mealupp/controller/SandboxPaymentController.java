package com.example.mealupp.controller;

import com.example.mealupp.dto.WebhookAck;
import com.example.mealupp.model.PaymentStatus;
import com.example.mealupp.service.PaymentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simula la página de la pasarela: aprueba o rechaza un pago de mentira. Existe solo si
 * mealup.payments.sandbox-enabled=true. En producción debe estar apagado.
 */
@RestController
@RequestMapping("/api/payments/sandbox")
@ConditionalOnProperty(name = "mealup.payments.sandbox-enabled", havingValue = "true")
public class SandboxPaymentController {

  private final PaymentService paymentService;

  public SandboxPaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/{reference}/approve")
  public WebhookAck approve(@PathVariable("reference") String reference) {
    paymentService.applyResult(reference, PaymentStatus.APPROVED, null);
    return new WebhookAck(true);
  }

  @PostMapping("/{reference}/decline")
  public WebhookAck decline(@PathVariable("reference") String reference) {
    paymentService.applyResult(reference, PaymentStatus.DECLINED, null);
    return new WebhookAck(true);
  }
}
