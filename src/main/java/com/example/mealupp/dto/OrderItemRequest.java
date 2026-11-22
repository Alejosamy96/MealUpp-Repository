package com.example.mealupp.dto;

import jakarta.validation.constraints.NotNull;

/** Un plato del pedido con cuántos almuerzos de ese plato se piden. */
public record OrderItemRequest(@NotNull Long dishId, @NotNull Integer quantity) {}
