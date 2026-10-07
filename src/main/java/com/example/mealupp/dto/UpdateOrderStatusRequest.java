package com.example.mealupp.dto;

import com.example.mealupp.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {}
