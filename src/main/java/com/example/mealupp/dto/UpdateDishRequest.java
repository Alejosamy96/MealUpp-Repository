package com.example.mealupp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** "dailyLimit" nulo quita el límite diario del plato. */
public record UpdateDishRequest(
    @NotBlank String name,
    String description,
    @NotNull BigDecimal price,
    @Min(1) Integer dailyLimit) {

  public UpdateDishRequest(String name, String description, BigDecimal price) {
    this(name, description, price, null);
  }
}
