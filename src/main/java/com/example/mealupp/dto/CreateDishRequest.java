package com.example.mealupp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateDishRequest(
    @NotBlank String name, String description, @NotNull BigDecimal price, Boolean available) {}
