package com.example.mealupp.exception;

/** Usuario o contraseña incorrectos (responde 401). */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException(String message) {
    super(message);
  }
}
