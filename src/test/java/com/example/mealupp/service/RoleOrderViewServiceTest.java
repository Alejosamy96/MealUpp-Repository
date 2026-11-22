package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.mealupp.dto.CashierOrderView;
import com.example.mealupp.dto.DeliveryOrderView;
import com.example.mealupp.dto.KitchenOrderView;
import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Dish;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.repository.OrderRepository;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pruebas unitarias de HU-06, criterio 2: cada rol ve solo los pedidos que necesita. */
@ExtendWith(MockitoExtension.class)
class RoleOrderViewServiceTest {

  private static final ZoneId ZONE = ZoneId.of("America/Bogota");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

  @Mock private OrderRepository orderRepository;

  private RoleOrderViewService service;
  private final Dish dish = new Dish("Pollo a la plancha", null, new BigDecimal("12000"), true);

  @BeforeEach
  void setUp() {
    Clock clock =
        Clock.fixed(LocalDateTime.of(TODAY, LocalTime.of(11, 0)).atZone(ZONE).toInstant(), ZONE);
    service = new RoleOrderViewService(orderRepository, clock);
  }

  private Order order(int quantity, OrderStatus status) {
    Order order =
        new Order(
            dish,
            quantity,
            "Valentina",
            TODAY,
            LocalTime.of(12, 0),
            DeliveryZone.SHOPPING_CENTER,
            "Local 12");
    order.setStatus(status);
    return order;
  }

  private void todayHasOneOrderOfEachStatus() {
    when(orderRepository.findByOrderDateOrderByDeliveryTimeAsc(TODAY))
        .thenReturn(
            List.of(
                order(1, OrderStatus.PENDING),
                order(2, OrderStatus.IN_PREPARATION),
                order(3, OrderStatus.DELIVERED),
                order(4, OrderStatus.CANCELLED),
                order(5, OrderStatus.READY)));
  }

  private static List<String> fields(Class<?> recordType) {
    return Arrays.stream(recordType.getRecordComponents()).map(RecordComponent::getName).toList();
  }

  @Test
  void kitchenOnlySeesWhatIsLeftToCook() {
    todayHasOneOrderOfEachStatus();

    assertThat(service.kitchenOrders())
        .extracting(KitchenOrderView::status)
        .containsExactly(OrderStatus.PENDING, OrderStatus.IN_PREPARATION);
  }

  @Test
  void kitchenDoneListsTheOrdersAlreadyReadyOrDelivered() {
    todayHasOneOrderOfEachStatus();

    assertThat(service.kitchenDoneOrders())
        .extracting(KitchenOrderView::status)
        .containsExactly(OrderStatus.DELIVERED, OrderStatus.READY);
  }

  @Test
  void cashierSeesEveryOrderOfTodayWithItsTotal() {
    todayHasOneOrderOfEachStatus();

    List<CashierOrderView> views = service.cashierOrders();

    assertThat(views).hasSize(5);
    assertThat(views.get(2).total()).isEqualByComparingTo("36000");
  }

  @Test
  void eachViewHidesWhatThatRoleDoesNotNeed() {
    assertThat(fields(KitchenOrderView.class))
        .doesNotContain("customerName", "deliveryZone", "deliveryDetail", "total");
    assertThat(fields(CashierOrderView.class)).doesNotContain("deliveryZone", "deliveryDetail");
  }

  @Test
  void deliveryOnlySeesOrdersReadyToGoWithoutPrices() {
    todayHasOneOrderOfEachStatus();

    assertThat(service.deliveryOrders(null)).extracting(DeliveryOrderView::quantity).hasSize(1);
    assertThat(fields(DeliveryOrderView.class)).doesNotContain("unitPrice", "total");
  }

  @Test
  void deliveredListsOnlyTheOrdersAlreadyDeliveredWithTheirData() {
    todayHasOneOrderOfEachStatus();

    assertThat(service.deliveredOrders(null))
        .extracting(DeliveryOrderView::status)
        .containsExactly(OrderStatus.DELIVERED);
    assertThat(service.deliveredOrders(null).get(0).customerName()).isEqualTo("Valentina");
  }

  @Test
  void deliveryCanFilterByZone() {
    when(orderRepository.findByOrderDateAndDeliveryZoneOrderByDeliveryTimeAsc(
            TODAY, DeliveryZone.FURNITURE_STREET))
        .thenReturn(List.of());

    assertThat(service.deliveryOrders(DeliveryZone.FURNITURE_STREET)).isEmpty();
  }
}
