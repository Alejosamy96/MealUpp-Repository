package com.example.mealupp.exception;

/** Se lanza cuando no existe el recurso pedido (responde 404). */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
