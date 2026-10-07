package com.example.mealupp.dto;

import java.time.LocalDate;
import java.util.List;

/** Pedidos del día ordenados por hora, con el total de almuerzos a preparar. */
public record DailyOrdersResponse(LocalDate date, int totalLunches, List<OrderResponse> orders) {}
