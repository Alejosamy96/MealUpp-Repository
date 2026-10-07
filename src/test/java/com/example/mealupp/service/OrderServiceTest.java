package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mealupp.config.OrderRulesProperties;
import com.example.mealupp.dto.CreateOrderRequest;
import com.example.mealupp.dto.DailyOrdersResponse;
import com.example.mealupp.dto.OrderResponse;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Dish;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.repository.DishRepository;
import com.example.mealupp.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pruebas unitarias de HU-02, HU-03 y HU-04. Las reglas son las de application.properties. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  private static final ZoneId ZONE = ZoneId.of("America/Bogota");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);
  private static final OrderRulesProperties RULES =
      new OrderRulesProperties(
          LocalTime.of(10, 30), LocalTime.of(11, 30), LocalTime.of(14, 0), 1, 10);

  @Mock private OrderRepository orderRepository;
  @Mock private DishRepository dishRepository;

  private Dish availableDish;

  @BeforeEach
  void setUp() {
    availableDish = new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true);
  }

  private OrderService serviceAt(LocalTime now) {
    Clock clock = Clock.fixed(LocalDateTime.of(TODAY, now).atZone(ZONE).toInstant(), ZONE);
    return new OrderService(orderRepository, dishRepository, RULES, clock);
  }

  private static CreateOrderRequest request(int quantity, LocalTime deliveryTime) {
    return new CreateOrderRequest(
        1L, quantity, "Valentina", deliveryTime, DeliveryZone.SHOPPING_CENTER, "Local 12");
  }

  private void dishExistsAndSaveWorks() {
    when(dishRepository.findById(1L)).thenReturn(Optional.of(availableDish));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  // ---------- HU-02: cantidad (valores límite) ----------

  @ParameterizedTest(name = "cantidad {0} se rechaza")
  @ValueSource(ints = {0, 11})
  void cp03_cp06_quantityOutsideRangeIsRejected(int quantity) {
    OrderService service = serviceAt(LocalTime.of(9, 0));

    assertThatThrownBy(() -> service.createOrder(request(quantity, LocalTime.of(12, 0))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cantidad");
    verify(orderRepository, never()).save(any());
  }

  @ParameterizedTest(name = "cantidad {0} se acepta")
  @ValueSource(ints = {1, 10})
  void cp04_cp05_quantityAtLimitsIsAccepted(int quantity) {
    dishExistsAndSaveWorks();

    OrderResponse response =
        serviceAt(LocalTime.of(9, 0)).createOrder(request(quantity, LocalTime.of(12, 0)));

    assertThat(response.quantity()).isEqualTo(quantity);
  }

  // ---------- HU-02: hora de entrega (valores límite) ----------

  @ParameterizedTest(name = "entrega a las {0} se rechaza")
  @ValueSource(strings = {"11:29", "14:01"})
  void cp07_deliveryTimeOutsideWindowIsRejected(String time) {
    OrderService service = serviceAt(LocalTime.of(9, 0));

    assertThatThrownBy(() -> service.createOrder(request(1, LocalTime.parse(time))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("hora de entrega");
  }

  @ParameterizedTest(name = "entrega a las {0} se acepta")
  @ValueSource(strings = {"11:30", "14:00"})
  void cp08_deliveryTimeAtWindowLimitsIsAccepted(String time) {
    dishExistsAndSaveWorks();

    OrderResponse response = serviceAt(LocalTime.of(9, 0)).createOrder(request(1, LocalTime.parse(time)));

    assertThat(response.deliveryTime()).isEqualTo(LocalTime.parse(time));
  }

  // ---------- HU-02: hora de corte (valores límite) ----------

  @Test
  void cp09a_orderOneMinuteBeforeCutoffIsAccepted() {
    dishExistsAndSaveWorks();

    OrderResponse response =
        serviceAt(LocalTime.of(10, 29)).createOrder(request(1, LocalTime.of(12, 0)));

    assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
  }

  @ParameterizedTest(name = "pedido a las {0} se rechaza")
  @ValueSource(strings = {"10:30", "10:31"})
  void cp09b_orderAtOrAfterCutoffIsRejected(String now) {
    OrderService service = serviceAt(LocalTime.parse(now));

    assertThatThrownBy(() -> service.createOrder(request(1, LocalTime.of(12, 0))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("antes de las");
  }

  // ---------- HU-02: plato (partición de equivalencias) ----------

  @Test
  void cp11a_unavailableDishIsRejected() {
    Dish unavailable = new Dish("Pasta", "Agotada", new BigDecimal("11000"), false);
    when(dishRepository.findById(1L)).thenReturn(Optional.of(unavailable));
    OrderService service = serviceAt(LocalTime.of(9, 0));

    assertThatThrownBy(() -> service.createOrder(request(1, LocalTime.of(12, 0))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("no está disponible");
  }

  @Test
  void cp11b_nonexistentDishIsRejected() {
    when(dishRepository.findById(1L)).thenReturn(Optional.empty());
    OrderService service = serviceAt(LocalTime.of(9, 0));

    assertThatThrownBy(() -> service.createOrder(request(1, LocalTime.of(12, 0))))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void cp12_validOrderIsSavedAsPendingWithAllItsData() {
    dishExistsAndSaveWorks();

    OrderResponse response =
        serviceAt(LocalTime.of(9, 0)).createOrder(request(2, LocalTime.of(12, 30)));

    assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
    assertThat(response.dishName()).isEqualTo("Pollo a la plancha");
    assertThat(response.customerName()).isEqualTo("Valentina");
    assertThat(response.orderDate()).isEqualTo(TODAY);
    assertThat(response.deliveryZone()).isEqualTo(DeliveryZone.SHOPPING_CENTER);
    assertThat(response.deliveryDetail()).isEqualTo("Local 12");
  }

  // ---------- HU-03 ----------

  private Order order(int quantity, LocalTime time, DeliveryZone zone, OrderStatus status) {
    Order order = new Order(availableDish, quantity, "Cliente", TODAY, time, zone, "Local 1");
    order.setStatus(status);
    return order;
  }

  @Test
  void cp13_totalLunchesExcludesCancelledOrders() {
    when(orderRepository.findByOrderDateOrderByDeliveryTimeAsc(TODAY))
        .thenReturn(
            List.of(
                order(2, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING),
                order(3, LocalTime.of(12, 30), DeliveryZone.FURNITURE_STREET, OrderStatus.CANCELLED),
                order(4, LocalTime.of(13, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.IN_PREPARATION)));

    DailyOrdersResponse response = serviceAt(LocalTime.of(11, 0)).getDailyOrders(null);

    assertThat(response.totalLunches()).isEqualTo(6);
    assertThat(response.orders()).hasSize(3);
  }

  @Test
  void cp14_ordersCanBeFilteredByZone() {
    when(orderRepository.findByOrderDateAndDeliveryZoneOrderByDeliveryTimeAsc(
            TODAY, DeliveryZone.FURNITURE_STREET))
        .thenReturn(
            List.of(order(5, LocalTime.of(12, 0), DeliveryZone.FURNITURE_STREET, OrderStatus.PENDING)));

    DailyOrdersResponse response =
        serviceAt(LocalTime.of(11, 0)).getDailyOrders(DeliveryZone.FURNITURE_STREET);

    assertThat(response.orders())
        .allMatch(o -> o.deliveryZone() == DeliveryZone.FURNITURE_STREET);
    assertThat(response.totalLunches()).isEqualTo(5);
  }

  // ---------- HU-04 ----------

  @Test
  void cp15b_validTransitionUpdatesStatus() {
    Order pending = order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(pending));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    OrderResponse response =
        serviceAt(LocalTime.of(11, 0)).updateStatus(7L, OrderStatus.IN_PREPARATION);

    assertThat(response.status()).isEqualTo(OrderStatus.IN_PREPARATION);
  }

  @Test
  void cp15c_skippingAStateIsRejected() {
    Order pending = order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(pending));
    OrderService service = serviceAt(LocalTime.of(11, 0));

    assertThatThrownBy(() -> service.updateStatus(7L, OrderStatus.DELIVERED))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(pending.getStatus()).isEqualTo(OrderStatus.PENDING);
  }

  @Test
  void cp16_finalOrderCannotBeModified() {
    Order delivered =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.DELIVERED);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(delivered));
    OrderService service = serviceAt(LocalTime.of(11, 0));

    assertThatThrownBy(() -> service.updateStatus(7L, OrderStatus.PENDING))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("ya no se puede modificar");
  }

  @Test
  void cp16b_unknownOrderReturnsNotFound() {
    when(orderRepository.findById(99L)).thenReturn(Optional.empty());
    OrderService service = serviceAt(LocalTime.of(11, 0));

    assertThatThrownBy(() -> service.updateStatus(99L, OrderStatus.IN_PREPARATION))
        .isInstanceOf(NotFoundException.class);
  }
}
