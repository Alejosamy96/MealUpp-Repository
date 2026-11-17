package com.example.mealupp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas periódicas. Se puede apagar con mealup.scheduling.enabled=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "mealup.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SchedulingConfig {}
