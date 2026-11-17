package com.example.mealupp.integration.payment;

import java.math.BigDecimal;

/**
 * Puerto hacia la pasarela de pagos. Cuando se elija un proveedor real (Wompi, ePayco, PayU...) se
 * escribe un adaptador nuevo; el resto del sistema no cambia.
 */
public interface PaymentGateway {

  /** Datos que se envían a la pasarela para crear una transacción. */
  record PaymentRequest(
      Long orderId, String reference, BigDecimal amount, String currency, String description) {}

  /** Respuesta de la pasarela: su referencia y la dirección donde el cliente paga. */
  record PaymentSession(String providerReference, String paymentUrl) {}

  PaymentSession createTransaction(PaymentRequest request);
}
