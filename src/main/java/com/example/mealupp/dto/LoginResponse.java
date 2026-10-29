package com.example.mealupp.dto;

import com.example.mealupp.model.StaffRole;
import java.time.LocalDateTime;
import java.util.List;

/** Respuesta del inicio de sesión. "sections" son las opciones que debe mostrar la pantalla. */
public record LoginResponse(
    String token,
    LocalDateTime expiresAt,
    String fullName,
    StaffRole role,
    List<String> sections) {}
