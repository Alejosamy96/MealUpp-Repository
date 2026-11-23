package com.example.mealupp.dto;

import com.example.mealupp.model.Dish;
import com.example.mealupp.service.MediaUrls;
import java.math.BigDecimal;

/**
 * Plato tal como lo ve el cliente en el menú. Las direcciones de la foto son nulas si no hay foto.
 * "soldOut" indica que ya no se puede pedir hoy y "remaining" cuántos quedan (nulo si no hay
 * límite).
 */
public record DishResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    String thumbnailUrl,
    String imageUrl,
    boolean soldOut,
    Integer remaining) {

  public static DishResponse from(Dish dish, MediaUrls urls) {
    return from(dish, urls, 0);
  }

  public static DishResponse from(Dish dish, MediaUrls urls, long soldToday) {
    return new DishResponse(
        dish.getId(),
        dish.getName(),
        dish.getDescription(),
        dish.getPrice(),
        urls.thumbnailUrl(dish.getImageKey()),
        urls.detailUrl(dish.getImageKey()),
        dish.soldOut(soldToday),
        dish.remaining(soldToday));
  }
}
