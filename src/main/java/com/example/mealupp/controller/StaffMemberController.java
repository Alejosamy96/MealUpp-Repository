package com.example.mealupp.controller;

import com.example.mealupp.dto.ChangeStaffRoleRequest;
import com.example.mealupp.dto.CreateStaffMemberRequest;
import com.example.mealupp.dto.StaffMemberResponse;
import com.example.mealupp.dto.UpdateStaffActiveRequest;
import com.example.mealupp.service.StaffAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administración del personal. Solo para el rol ADMIN (ver SecurityConfig). */
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

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public StaffMemberResponse create(@Valid @RequestBody CreateStaffMemberRequest request) {
    return staffAdminService.create(request);
  }

  @PatchMapping("/{id}/role")
  public StaffMemberResponse changeRole(
      @PathVariable("id") Long id, @Valid @RequestBody ChangeStaffRoleRequest request) {
    return staffAdminService.changeRole(id, request.role());
  }

  @PatchMapping("/{id}/active")
  public StaffMemberResponse setActive(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateStaffActiveRequest request) {
    return staffAdminService.setActive(id, request.active());
  }
}
