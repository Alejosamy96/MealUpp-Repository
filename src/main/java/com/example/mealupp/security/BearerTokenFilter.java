package com.example.mealupp.security;

import com.example.mealupp.service.StaffAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Lee "Authorization: Bearer <token>" y, si el token es válido, autentica a la persona. */
public class BearerTokenFilter extends OncePerRequestFilter {

  private static final String PREFIX = "Bearer ";

  private final StaffAuthService authService;

  public BearerTokenFilter(StaffAuthService authService) {
    this.authService = authService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(PREFIX)) {
      authService
          .findPrincipalByToken(header.substring(PREFIX.length()))
          .ifPresent(
              principal ->
                  SecurityContextHolder.getContext()
                      .setAuthentication(
                          new UsernamePasswordAuthenticationToken(
                              principal,
                              null,
                              List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())))));
    }
    chain.doFilter(request, response);
  }
}
