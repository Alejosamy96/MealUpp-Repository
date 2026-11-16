package com.example.mealupp.service;

import com.example.mealupp.dto.DishResponse;
import com.example.mealupp.dto.MenuResponse;
import com.example.mealupp.repository.DishRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** HU-01: consultar el menú del día. */
@Service
public class MenuService {

  static final String EMPTY_MENU_MESSAGE = "No hay menú disponible hoy";

  private final DishRepository dishRepository;
  private final MediaUrls mediaUrls;

  public MenuService(DishRepository dishRepository, MediaUrls mediaUrls) {
    this.dishRepository = dishRepository;
    this.mediaUrls = mediaUrls;
  }

  public MenuResponse getMenu() {
    List<DishResponse> dishes =
        dishRepository.findByAvailableTrue().stream()
            .map(dish -> DishResponse.from(dish, mediaUrls))
            .toList();
    String message = dishes.isEmpty() ? EMPTY_MENU_MESSAGE : null;
    return new MenuResponse(dishes, message);
  }
}
