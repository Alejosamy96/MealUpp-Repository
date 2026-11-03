package com.example.mealupp.dto;

import com.example.mealupp.model.StaffRole;
import jakarta.validation.constraints.NotNull;

public record ChangeStaffRoleRequest(@NotNull StaffRole role) {}
