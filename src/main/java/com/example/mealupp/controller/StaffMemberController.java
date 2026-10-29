package com.example.mealupp.controller;

import com.example.mealupp.dto.ChangeStaffRoleRequest;
import com.example.mealupp.dto.StaffMemberResponse;
import com.example.mealupp.service.StaffAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU 6: asignación de roles. Solo para el rol ADMIN (ver SecurityConfig, /api/staff/**). */
@RestController
@RequestMapping("/api/staff/members")
public class StaffMemberController {

  private final StaffAdminService staffAdminService;

  public StaffMemberController(StaffAdminService staffAdminService) {
    this.staffAdminService = staffAdminService;
  }

  @GetMapping
  public List<StaffMemberResponse> listAll() {
    return staffAdminService.listAll();
  }

  @PatchMapping("/{id}/role")
  public StaffMemberResponse changeRole(
      @PathVariable("id") Long id, @Valid @RequestBody ChangeStaffRoleRequest request) {
    return staffAdminService.changeRole(id, request.role());
  }
}
