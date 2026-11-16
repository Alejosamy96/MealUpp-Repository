package com.example.mealupp.integration.payment;

import java.util.UUID;

/**
 * Pasarela simulada para desarrollo y demostraciones. No cobra nada: genera una referencia y una
 * dirección a una página del frontend desde la que se aprueba o rechaza el pago de mentira.
 */
public final class SandboxPaymentGateway implements PaymentGateway {

  private final String checkoutUrl;

  public SandboxPaymentGateway(String checkoutUrl) {
    this.checkoutUrl = checkoutUrl;
  }

  @Override
  public PaymentSession createTransaction(PaymentRequest request) {
    String reference = "SBX-" + UUID.randomUUID();
    return new PaymentSession(reference, checkoutUrl + "?reference=" + reference);
  }
}
