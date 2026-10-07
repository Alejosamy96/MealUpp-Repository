package com.example.mealupp.dto;

import com.example.mealupp.model.Dish;
import java.math.BigDecimal;

public record DishResponse(Long id, String name, String description, BigDecimal price) {

  public static DishResponse from(Dish dish) {
    return new DishResponse(dish.getId(), dish.getName(), dish.getDescription(), dish.getPrice());
  }
}
