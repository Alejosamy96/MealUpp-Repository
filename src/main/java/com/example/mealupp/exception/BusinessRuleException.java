package com.example.mealupp.exception;

/** Se lanza cuando una solicitud incumple una regla de negocio (responde 400). */
public class BusinessRuleException extends RuntimeException {

  public BusinessRuleException(String message) {
    super(message);
  }
}
