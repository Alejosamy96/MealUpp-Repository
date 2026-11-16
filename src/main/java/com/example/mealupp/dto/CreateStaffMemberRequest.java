package com.example.mealupp.dto;

import com.example.mealupp.model.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStaffMemberRequest(
    @NotBlank String username,
    @NotBlank String fullName,
    @NotBlank String password,
    @NotNull StaffRole role) {}
