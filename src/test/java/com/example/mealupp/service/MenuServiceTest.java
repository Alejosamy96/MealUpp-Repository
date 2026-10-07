package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.mealupp.dto.MenuResponse;
import com.example.mealupp.model.Dish;
import com.example.mealupp.repository.DishRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pruebas unitarias de HU-01. */
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

  @Mock private DishRepository dishRepository;

  @InjectMocks private MenuService menuService;

  @Test
  void cp01_menuShowsAvailableDishesWithNameDescriptionAndPrice() {
    Dish dish = new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true);
    when(dishRepository.findByAvailableTrue()).thenReturn(List.of(dish));

    MenuResponse menu = menuService.getMenu();

    assertThat(menu.dishes()).hasSize(1);
    assertThat(menu.dishes().get(0).name()).isEqualTo("Pollo a la plancha");
    assertThat(menu.dishes().get(0).description()).isEqualTo("Con arroz");
    assertThat(menu.dishes().get(0).price()).isEqualByComparingTo("12000");
    assertThat(menu.message()).isNull();
  }

  @Test
  void cp02_emptyMenuReturnsMessage() {
    when(dishRepository.findByAvailableTrue()).thenReturn(List.of());

    MenuResponse menu = menuService.getMenu();

    assertThat(menu.dishes()).isEmpty();
    assertThat(menu.message()).isEqualTo("No hay menú disponible hoy");
  }
}
