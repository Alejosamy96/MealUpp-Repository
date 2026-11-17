package com.example.mealupp.exception;

/** La firma del webhook de pagos no es válida (responde 401). */
public class InvalidSignatureException extends RuntimeException {

  public InvalidSignatureException(String message) {
    super(message);
  }
}
