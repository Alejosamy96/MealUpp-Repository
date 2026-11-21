package com.example.mealupp.exception;

/** La pasarela de pagos no respondió o rechazó la petición. */
public class PaymentGatewayException extends RuntimeException {

  public PaymentGatewayException(String message) {
    super(message);
  }

  public PaymentGatewayException(String message, Throwable cause) {
    super(message, cause);
  }
}
