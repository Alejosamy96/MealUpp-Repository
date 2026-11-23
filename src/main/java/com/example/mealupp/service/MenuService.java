package com.example.mealupp.service;

import com.example.mealupp.dto.DishResponse;
import com.example.mealupp.dto.MenuResponse;
import com.example.mealupp.repository.DishRepository;
import com.example.mealupp.repository.OrderRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/** HU-01: consultar el menú del día. Los platos con cupo agotado salen marcados como agotados. */
@Service
public class MenuService {

  static final String EMPTY_MENU_MESSAGE = "No hay menú disponible hoy";

  private final DishRepository dishRepository;
  private final OrderRepository orderRepository;
  private final MediaUrls mediaUrls;
  private final Clock clock;

  public MenuService(
      DishRepository dishRepository,
      OrderRepository orderRepository,
      MediaUrls mediaUrls,
      Clock clock) {
    this.dishRepository = dishRepository;
    this.orderRepository = orderRepository;
    this.mediaUrls = mediaUrls;
    this.clock = clock;
  }

  public MenuResponse getMenu() {
    LocalDate today = LocalDate.now(clock);
    List<DishResponse> dishes =
        dishRepository.findByAvailableTrue().stream()
            .map(
                dish ->
                    DishResponse.from(
                        dish, mediaUrls, orderRepository.soldQuantity(dish.getId(), today)))
            .toList();
    String message = dishes.isEmpty() ? EMPTY_MENU_MESSAGE : null;
    return new MenuResponse(dishes, message);
  }
}
