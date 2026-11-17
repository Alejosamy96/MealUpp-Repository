package com.example.mealupp.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStaffActiveRequest(@NotNull Boolean active) {}
