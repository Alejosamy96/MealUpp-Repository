package com.example.mealupp.controller;

import com.example.mealupp.dto.LoginRequest;
import com.example.mealupp.dto.LoginResponse;
import com.example.mealupp.service.StaffAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HU-05: inicio y cierre de sesión del personal. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final StaffAuthService authService;

  public AuthController(StaffAuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.username(), request.password());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@RequestHeader("Authorization") String authorization) {
    authService.logout(authorization.substring("Bearer ".length()));
  }
}
