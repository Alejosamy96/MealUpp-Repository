package com.example.mealupp.dto;

import com.example.mealupp.model.Dish;
import com.example.mealupp.service.MediaUrls;
import java.math.BigDecimal;

/** Plato visto por la administradora: a diferencia del menú público, muestra si está disponible. */
public record StaffDishResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    boolean available,
    String thumbnailUrl,
    String imageUrl) {

  public static StaffDishResponse from(Dish dish, MediaUrls urls) {
    return new StaffDishResponse(
        dish.getId(),
        dish.getName(),
        dish.getDescription(),
        dish.getPrice(),
        dish.isAvailable(),
        urls.thumbnailUrl(dish.getImageKey()),
        urls.detailUrl(dish.getImageKey()));
  }
}
