package com.example.mealupp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** "dailyLimit" es el máximo de almuerzos de este plato por día; si no se envía, no hay límite. */
public record CreateDishRequest(
    @NotBlank String name,
    String description,
    @NotNull BigDecimal price,
    Boolean available,
    @Min(1) Integer dailyLimit) {

  public CreateDishRequest(String name, String description, BigDecimal price, Boolean available) {
    this(name, description, price, available, null);
  }
}
