package com.example.mealupp.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** El reloj se inyecta para poder probar las reglas de horario con una hora fija. */
@Configuration
public class ClockConfig {

  @Bean
  public Clock clock(@Value("${mealup.time-zone}") String timeZone) {
    return Clock.system(ZoneId.of(timeZone));
  }
}
