package com.example.mealupp.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/** HU-06 (criterio 3): si el rol no alcanza, se responde 403 con un mensaje claro. */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

  static final String MESSAGE = "No tienes permiso para entrar a esta sección";

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write("{\"message\":\"" + MESSAGE + "\"}");
  }
}
