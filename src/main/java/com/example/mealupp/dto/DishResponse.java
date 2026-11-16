package com.example.mealupp.dto;

import com.example.mealupp.model.Dish;
import com.example.mealupp.service.MediaUrls;
import java.math.BigDecimal;

/**
 * Plato tal como lo ve el cliente en el menú. Las direcciones de la foto son nulas si no hay foto.
 */
public record DishResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    String thumbnailUrl,
    String imageUrl) {

  public static DishResponse from(Dish dish, MediaUrls urls) {
    return new DishResponse(
        dish.getId(),
        dish.getName(),
        dish.getDescription(),
        dish.getPrice(),
        urls.thumbnailUrl(dish.getImageKey()),
        urls.detailUrl(dish.getImageKey()));
  }
}
