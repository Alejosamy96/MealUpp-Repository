package com.example.mealupp.security;

import com.example.mealupp.model.StaffRole;

/** Quién hizo la petición: se guarda en el contexto de seguridad cuando el token es válido. */
public record StaffPrincipal(String username, StaffRole role) {}
