package com.example.mealupp.dto;

import com.example.mealupp.model.StaffRole;
import java.time.LocalDateTime;

public record LoginResponse(
    String token, LocalDateTime expiresAt, String fullName, StaffRole role) {}
