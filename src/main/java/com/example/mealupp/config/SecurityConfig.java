package com.example.mealupp.config;

import com.example.mealupp.security.BearerTokenFilter;
import com.example.mealupp.security.JsonAccessDeniedHandler;
import com.example.mealupp.service.StaffAuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * HU-05: solo el personal registrado entra a /api/staff/**. Menú, pedidos y login son públicos.
 *
 * <p>HU-06: cocina entra solo a /api/staff/kitchen, caja solo a /api/staff/cashier, y todo lo demás
 * de /api/staff es solo para ADMIN. ADMIN también puede entrar a las vistas de cocina y caja.
 *
 * <p>DEF-01: el personal se autentica con un token en la cabecera Authorization. El navegador no
 * envía esa cabecera por su cuenta (como sí pasaba con HTTP Basic), así que otra página no puede
 * hacer peticiones a nombre del personal. Por eso la protección CSRF no aplica a esta API sin
 * sesiones ni cookies.
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, StaffAuthService authService) {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler(new JsonAccessDeniedHandler()))
        .addFilterBefore(
            new BearerTokenFilter(authService), UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/menu", "/api/orders", "/api/auth/login", "/error")
                    .permitAll()
                    .requestMatchers("/api/auth/logout")
                    .authenticated()
                    .requestMatchers("/api/staff/kitchen/**")
                    .hasAnyRole("KITCHEN", "ADMIN")
                    .requestMatchers("/api/staff/cashier/**")
                    .hasAnyRole("CASHIER", "ADMIN")
                    .requestMatchers("/api/staff/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .denyAll());
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
