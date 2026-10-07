package com.example.mealupp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/** HU-05: solo el personal con rol STAFF accede a /api/staff/**. Menú y pedidos son públicos. */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/staff/**")
                    .hasRole("STAFF")
                    .requestMatchers("/api/menu", "/api/orders", "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .httpBasic(Customizer.withDefaults());
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  /** La contraseña llega por variable de entorno (DoD #7), nunca escrita en el código. */
  @Bean
  public UserDetailsService userDetailsService(
      @Value("${mealup.staff.username}") String username,
      @Value("${mealup.staff.password}") String password,
      PasswordEncoder encoder) {
    return new InMemoryUserDetailsManager(
        User.withUsername(username).password(encoder.encode(password)).roles("STAFF").build());
  }
}
