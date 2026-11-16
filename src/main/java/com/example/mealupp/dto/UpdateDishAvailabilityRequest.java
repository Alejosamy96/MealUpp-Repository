package com.example.mealupp.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateDishAvailabilityRequest(@NotNull Boolean available) {}
