package com.example.mealupp.config;

import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.StaffMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Si no hay nadie registrado, crea la cuenta de la administradora con el usuario y la contraseña de
 * las variables de entorno STAFF_USERNAME y STAFF_PASSWORD (DoD #7).
 */
@Configuration
public class StaffBootstrap {

  @Bean
  public ApplicationRunner createFirstAdmin(
      StaffMemberRepository repository,
      PasswordEncoder encoder,
      @Value("${mealup.staff.username}") String username,
      @Value("${mealup.staff.password}") String password) {
    return args -> {
      if (repository.count() == 0) {
        repository.save(
            new StaffMember(username, "Administradora", encoder.encode(password), StaffRole.ADMIN));
      }
    };
  }
}
