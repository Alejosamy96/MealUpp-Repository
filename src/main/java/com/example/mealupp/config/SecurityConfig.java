package com.example.mealupp.config;

import com.example.mealupp.security.BearerTokenFilter;
import com.example.mealupp.security.JsonAccessDeniedHandler;
import com.example.mealupp.security.JwtService;
import com.example.mealupp.service.StaffAuthService;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Quién puede entrar a qué.
 *
 * <p>Públicos: el menú, la configuración, el login, crear y consultar un pedido (con su código), el
 * webhook de la pasarela (se valida por firma), las fotos y la conexión WebSocket (se valida al
 * conectar). Cada rol del personal entra solo a lo suyo: cocina a /api/staff/kitchen, caja a
 * /api/staff/cashier, entrega a /api/staff/delivery, y todo lo demás de /api/staff es de ADMIN.
 * Cambiar el estado de un pedido lo puede intentar cualquier rol, pero el servicio revisa qué
 * cambio le corresponde a cada uno.
 *
 * <p>Sin CSRF: la API no usa cookies ni sesión. El token viaja en la cabecera Authorization, que el
 * navegador no envía por su cuenta, así que otra página no puede actuar a nombre del personal.
 */
@Configuration
public class SecurityConfig {

  private static final String ADMIN = "ADMIN";

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, StaffAuthService authService) {
    http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler(new JsonAccessDeniedHandler()))
        .addFilterBefore(
            new BearerTokenFilter(authService), UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/menu",
                        "/api/config",
                        "/api/auth/login",
                        "/error",
                        "/ws/**",
                        "/media/**",
                        "/actuator/health",
                        "/actuator/health/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/orders",
                        "/api/orders/*/pay",
                        "/api/payments/webhook",
                        "/api/payments/mercadopago/webhook")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/orders/*")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/payments/sandbox/*/approve",
                        "/api/payments/sandbox/*/decline")
                    .permitAll()
                    .requestMatchers("/api/auth/logout")
                    .authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/staff/orders/*/status")
                    .hasAnyRole(ADMIN, "KITCHEN", "CASHIER", "DELIVERY")
                    .requestMatchers("/api/staff/kitchen/**")
                    .hasAnyRole("KITCHEN", ADMIN)
                    .requestMatchers("/api/staff/cashier/**")
                    .hasAnyRole("CASHIER", ADMIN)
                    .requestMatchers("/api/staff/delivery/**")
                    .hasAnyRole("DELIVERY", ADMIN)
                    .requestMatchers("/api/staff/**")
                    .hasRole(ADMIN)
                    .anyRequest()
                    .denyAll());
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  /** La clave de firma llega por variable de entorno (JWT_SECRET), nunca escrita en el código. */
  @Bean
  public JwtService jwtService(@Value("${mealup.security.jwt-secret}") String secret) {
    return new JwtService(secret);
  }

  /** Orígenes del frontend que pueden llamar a la API desde el navegador. */
  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${mealup.cors.allowed-origins}") String origins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(
        Arrays.stream(origins.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
