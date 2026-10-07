package com.example.mealupp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MealUppRepositoryApplication {

  public static void main(String[] args) {
    SpringApplication.run(MealUppRepositoryApplication.class, args);
  }
}
