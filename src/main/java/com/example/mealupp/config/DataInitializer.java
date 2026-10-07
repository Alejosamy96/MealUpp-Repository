package com.example.mealupp.config;

import com.example.mealupp.model.Dish;
import com.example.mealupp.repository.DishRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Carga platos de ejemplo cuando la base está vacía, para probar a mano. */
@Configuration
@Profile("!test")
public class DataInitializer {

  @Bean
  public CommandLineRunner loadSampleMenu(DishRepository dishRepository) {
    return args -> {
      if (dishRepository.count() == 0) {
        dishRepository.save(
            new Dish("Pollo a la plancha", "Con ensalada y arroz", new BigDecimal("12000"), true));
        dishRepository.save(
            new Dish("Carne en salsa", "Con patacón y ensalada", new BigDecimal("13000"), true));
        dishRepository.save(
            new Dish("Pasta Alfredo", "Con vegetales", new BigDecimal("11000"), true));
      }
    };
  }
}
