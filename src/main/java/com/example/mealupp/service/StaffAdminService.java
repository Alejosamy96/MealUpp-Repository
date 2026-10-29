package com.example.mealupp.service;

import com.example.mealupp.dto.StaffMemberResponse;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.StaffMemberRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU 6: la administradora asigna o cambia el rol de cada miembro del personal. Esta historia no
 * crea ni elimina usuarios (criterio 4).
 */
@Service
public class StaffAdminService {

  private final StaffMemberRepository staffRepository;

  public StaffAdminService(StaffMemberRepository staffRepository) {
    this.staffRepository = staffRepository;
  }

  @Transactional(readOnly = true)
  public List<StaffMemberResponse> listAll() {
    return staffRepository.findAll().stream().map(StaffMemberResponse::from).toList();
  }

  @Transactional
  public StaffMemberResponse changeRole(Long memberId, StaffRole newRole) {
    StaffMember member =
        staffRepository
            .findById(memberId)
            .orElseThrow(() -> new NotFoundException("La persona no está registrada"));
    if (isLastAdmin(member) && newRole != StaffRole.ADMIN) {
      throw new BusinessRuleException("Debe quedar al menos una persona con rol ADMIN");
    }
    member.setRole(newRole);
    return StaffMemberResponse.from(staffRepository.save(member));
  }

  private boolean isLastAdmin(StaffMember member) {
    return member.getRole() == StaffRole.ADMIN
        && staffRepository.countByRoleAndActiveTrue(StaffRole.ADMIN) <= 1;
  }
}
