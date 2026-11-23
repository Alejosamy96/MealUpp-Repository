package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.mealupp.dto.MenuResponse;
import com.example.mealupp.model.Dish;
import com.example.mealupp.repository.DishRepository;
import com.example.mealupp.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pruebas unitarias de HU-01. */
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

  @Mock private DishRepository dishRepository;

  @Mock private OrderRepository orderRepository;

  @Mock private MediaUrls mediaUrls;

  private MenuService menuService;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), ZoneId.of("America/Bogota"));
    menuService = new MenuService(dishRepository, orderRepository, mediaUrls, clock);
  }

  @Test
  void stock01_aDishWithADailyLimitShowsHowManyAreLeft() {
    Dish dish = new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true);
    dish.setDailyLimit(10);
    when(dishRepository.findByAvailableTrue()).thenReturn(List.of(dish));
    when(orderRepository.soldQuantity(any(), any())).thenReturn(4L);

    MenuResponse menu = menuService.getMenu();

    assertThat(menu.dishes().get(0).remaining()).isEqualTo(6);
    assertThat(menu.dishes().get(0).soldOut()).isFalse();
  }

  @Test
  void stock02_aDishWithNothingLeftComesOutAsSoldOut() {
    Dish dish = new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true);
    dish.setDailyLimit(10);
    when(dishRepository.findByAvailableTrue()).thenReturn(List.of(dish));
    when(orderRepository.soldQuantity(any(), any())).thenReturn(10L);

    MenuResponse menu = menuService.getMenu();

    assertThat(menu.dishes().get(0).remaining()).isZero();
    assertThat(menu.dishes().get(0).soldOut()).isTrue();
  }

  @Test
  void stock03_aDishWithoutLimitIsNeverSoldOutByStock() {
    Dish dish = new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true);
    when(dishRepository.findByAvailableTrue()).thenReturn(List.of(dish));

    MenuResponse menu = menuService.getMenu();

    assertThat(menu.dishes().get(0).remaining()).isNull();
    assertThat(menu.dishes().get(0).soldOut()).isFalse();
  }

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

  @Test
  void menu_showsThePhotoAddressesOfTheDish() {
    Dish dish = new Dish("Pasta", "Con vegetales", new BigDecimal("11000"), true);
    dish.setImageKey("dishes/3/ab12cd");
    when(dishRepository.findByAvailableTrue()).thenReturn(List.of(dish));
    when(mediaUrls.thumbnailUrl("dishes/3/ab12cd")).thenReturn("/media/dishes/3/ab12cd_thumb.jpg");
    when(mediaUrls.detailUrl("dishes/3/ab12cd")).thenReturn("/media/dishes/3/ab12cd_detail.jpg");

    MenuResponse menu = menuService.getMenu();

    assertThat(menu.dishes().get(0).thumbnailUrl()).isEqualTo("/media/dishes/3/ab12cd_thumb.jpg");
    assertThat(menu.dishes().get(0).imageUrl()).isEqualTo("/media/dishes/3/ab12cd_detail.jpg");
  }
}
