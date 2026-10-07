package com.example.mealupp.dto;

import java.util.List;

/** Menú del día. El mensaje solo aparece cuando no hay platos disponibles. */
public record MenuResponse(List<DishResponse> dishes, String message) {}
