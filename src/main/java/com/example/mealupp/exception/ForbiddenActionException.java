package com.example.mealupp.exception;

/** El rol de la persona no le permite hacer esta acción (responde 403). */
public class ForbiddenActionException extends RuntimeException {

  public ForbiddenActionException(String message) {
    super(message);
  }
}
