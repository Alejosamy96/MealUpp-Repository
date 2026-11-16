package com.example.mealupp.service;

import com.example.mealupp.dto.CreateStaffMemberRequest;
import com.example.mealupp.dto.StaffMemberResponse;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.StaffMemberRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** La administradora registra al personal, asigna roles y activa o desactiva cuentas. */
@Service
public class StaffAdminService {

  static final int MIN_USERNAME = 3;
  static final int MAX_USERNAME = 30;
  static final int MIN_PASSWORD = 8;
  static final String LAST_ADMIN = "Debe quedar al menos una persona con rol ADMIN";

  private final StaffMemberRepository staffRepository;
  private final PasswordEncoder passwordEncoder;

  public StaffAdminService(StaffMemberRepository staffRepository, PasswordEncoder passwordEncoder) {
    this.staffRepository = staffRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public List<StaffMemberResponse> listAll() {
    return staffRepository.findAll().stream().map(StaffMemberResponse::from).toList();
  }

  @Transactional
  public StaffMemberResponse create(CreateStaffMemberRequest request) {
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    validateUsername(username);
    if (request.password().length() < MIN_PASSWORD) {
      throw new BusinessRuleException(
          "La contraseña debe tener al menos " + MIN_PASSWORD + " caracteres");
    }
    if (staffRepository.existsByUsername(username)) {
      throw new BusinessRuleException("Ese usuario ya está registrado");
    }
    StaffMember member =
        new StaffMember(
            username,
            request.fullName().trim(),
            passwordEncoder.encode(request.password()),
            request.role());
    return StaffMemberResponse.from(staffRepository.save(member));
  }

  @Transactional
  public StaffMemberResponse changeRole(Long memberId, StaffRole newRole) {
    StaffMember member = find(memberId);
    if (isLastActiveAdmin(member) && newRole != StaffRole.ADMIN) {
      throw new BusinessRuleException(LAST_ADMIN);
    }
    member.setRole(newRole);
    return StaffMemberResponse.from(staffRepository.save(member));
  }

  @Transactional
  public StaffMemberResponse setActive(Long memberId, boolean active) {
    StaffMember member = find(memberId);
    if (!active && isLastActiveAdmin(member)) {
      throw new BusinessRuleException(LAST_ADMIN);
    }
    member.setActive(active);
    return StaffMemberResponse.from(staffRepository.save(member));
  }

  private StaffMember find(Long memberId) {
    return staffRepository
        .findById(memberId)
        .orElseThrow(() -> new NotFoundException("La persona no está registrada"));
  }

  private boolean isLastActiveAdmin(StaffMember member) {
    return member.getRole() == StaffRole.ADMIN
        && member.isActive()
        && staffRepository.countByRoleAndActiveTrue(StaffRole.ADMIN) <= 1;
  }

  private static void validateUsername(String username) {
    if (username.length() < MIN_USERNAME || username.length() > MAX_USERNAME) {
      throw new BusinessRuleException(
          "El usuario debe tener entre " + MIN_USERNAME + " y " + MAX_USERNAME + " caracteres");
    }
    if (!username.matches("[a-z0-9._-]+")) {
      throw new BusinessRuleException(
          "El usuario solo puede tener letras minúsculas, números, punto, guion y guion bajo");
    }
  }
}
