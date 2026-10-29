package com.example.mealupp.dto;

import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;

/** Datos de una persona del personal. Nunca incluye la contraseña ni su hash. */
public record StaffMemberResponse(
    Long id, String username, String fullName, StaffRole role, boolean active) {

  public static StaffMemberResponse from(StaffMember member) {
    return new StaffMemberResponse(
        member.getId(),
        member.getUsername(),
        member.getFullName(),
        member.getRole(),
        member.isActive());
  }
}
