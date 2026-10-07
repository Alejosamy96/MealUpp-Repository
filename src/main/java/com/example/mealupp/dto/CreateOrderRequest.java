package com.example.mealupp.dto;

import com.example.mealupp.model.DeliveryZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record CreateOrderRequest(
    @NotNull Long dishId,
    @NotNull Integer quantity,
    @NotBlank String customerName,
    @NotNull LocalTime deliveryTime,
    @NotNull DeliveryZone deliveryZone,
    @NotBlank String deliveryDetail) {}
