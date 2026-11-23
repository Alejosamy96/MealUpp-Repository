package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mealupp.config.OrderRulesProperties;
import com.example.mealupp.config.PaymentProperties;
import com.example.mealupp.dto.CreateOrderRequest;
import com.example.mealupp.dto.CreatedOrderResponse;
import com.example.mealupp.dto.DailyOrdersResponse;
import com.example.mealupp.dto.OrderItemRequest;
import com.example.mealupp.dto.OrderResponse;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.ForbiddenActionException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.integration.notification.KitchenNotifier;
import com.example.mealupp.integration.payment.PaymentGateway;
import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Dish;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.model.StaffRole;
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
import org.springframework.test.util.ReflectionTestUtils;

/** Pruebas unitarias de HU-02, HU-03 y HU-04. Las reglas son las de application.properties. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  private static final ZoneId ZONE = ZoneId.of("America/Bogota");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);
  private static final OrderRulesProperties RULES =
      new OrderRulesProperties(
          LocalTime.of(10, 30), LocalTime.of(11, 30), LocalTime.of(14, 0), 1, 10, 100);
  private static final PaymentProperties PAYMENTS_OFF =
      new PaymentProperties(false, "sandbox", "secreto", 15, "COP", "http://localhost/pago");
  private static final PaymentProperties PAYMENTS_ON =
      new PaymentProperties(true, "sandbox", "secreto", 15, "COP", "http://localhost/pago");

  @Mock private OrderRepository orderRepository;
  @Mock private DishRepository dishRepository;
  @Mock private PaymentService paymentService;
  @Mock private OrderConfirmationService confirmationService;
  @Mock private KitchenTicketService ticketService;
  @Mock private KitchenNotifier notifier;

  private Dish availableDish;

  @BeforeEach
  void setUp() {
    availableDish = new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true);
  }

  private OrderService serviceAt(LocalTime now) {
    return serviceAt(now, PAYMENTS_OFF);
  }

  private OrderService serviceAt(LocalTime now, PaymentProperties payments) {
    Clock clock = Clock.fixed(LocalDateTime.of(TODAY, now).atZone(ZONE).toInstant(), ZONE);
    return new OrderService(
        orderRepository,
        dishRepository,
        RULES,
        payments,
        clock,
        paymentService,
        confirmationService,
        ticketService,
        notifier);
  }

  private static CreateOrderRequest request(int quantity, LocalTime deliveryTime) {
    return new CreateOrderRequest(
        1L, quantity, null, "Valentina", deliveryTime, DeliveryZone.SHOPPING_CENTER, "Local 12");
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
    CreateOrderRequest invalid = request(quantity, LocalTime.of(12, 0));

    assertThatThrownBy(() -> service.createOrder(invalid))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cantidad");
    verify(orderRepository, never()).save(any());
  }

  @ParameterizedTest(name = "cantidad {0} se acepta")
  @ValueSource(ints = {1, 10})
  void cp04_cp05_quantityAtLimitsIsAccepted(int quantity) {
    dishExistsAndSaveWorks();

    CreatedOrderResponse response =
        serviceAt(LocalTime.of(9, 0)).createOrder(request(quantity, LocalTime.of(12, 0)));

    assertThat(response.quantity()).isEqualTo(quantity);
  }

  // ---------- HU-02: hora de entrega (valores límite) ----------

  @ParameterizedTest(name = "entrega a las {0} se rechaza")
  @ValueSource(strings = {"11:29", "14:01"})
  void cp07_deliveryTimeOutsideWindowIsRejected(String time) {
    OrderService service = serviceAt(LocalTime.of(9, 0));
    CreateOrderRequest invalid = request(1, LocalTime.parse(time));

    assertThatThrownBy(() -> service.createOrder(invalid))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("hora de entrega");
  }

  @ParameterizedTest(name = "entrega a las {0} se acepta")
  @ValueSource(strings = {"11:30", "14:00"})
  void cp08_deliveryTimeAtWindowLimitsIsAccepted(String time) {
    dishExistsAndSaveWorks();

    CreatedOrderResponse response =
        serviceAt(LocalTime.of(9, 0)).createOrder(request(1, LocalTime.parse(time)));

    assertThat(response.deliveryTime()).isEqualTo(LocalTime.parse(time));
  }

  // ---------- HU-02: hora de corte (valores límite) ----------

  @Test
  void cp09a_orderOneMinuteBeforeCutoffIsAccepted() {
    dishExistsAndSaveWorks();

    CreatedOrderResponse response =
        serviceAt(LocalTime.of(10, 29)).createOrder(request(1, LocalTime.of(12, 0)));

    assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
  }

  @ParameterizedTest(name = "pedido a las {0} se rechaza")
  @ValueSource(strings = {"10:30", "10:31"})
  void cp09b_orderAtOrAfterCutoffIsRejected(String now) {
    OrderService service = serviceAt(LocalTime.parse(now));
    CreateOrderRequest late = request(1, LocalTime.of(12, 0));

    assertThatThrownBy(() -> service.createOrder(late))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("antes de las");
  }

  // ---------- HU-02: plato (partición de equivalencias) ----------

  @Test
  void cp11a_unavailableDishIsRejected() {
    Dish unavailable = new Dish("Pasta", "Agotada", new BigDecimal("11000"), false);
    when(dishRepository.findById(1L)).thenReturn(Optional.of(unavailable));
    OrderService service = serviceAt(LocalTime.of(9, 0));
    CreateOrderRequest order = request(1, LocalTime.of(12, 0));

    assertThatThrownBy(() -> service.createOrder(order))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("no está disponible");
  }

  @Test
  void cp11b_nonexistentDishIsRejected() {
    when(dishRepository.findById(1L)).thenReturn(Optional.empty());
    OrderService service = serviceAt(LocalTime.of(9, 0));
    CreateOrderRequest order = request(1, LocalTime.of(12, 0));

    assertThatThrownBy(() -> service.createOrder(order)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void cp12_validOrderIsSavedAsPendingWithAllItsData() {
    dishExistsAndSaveWorks();

    CreatedOrderResponse response =
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
                order(
                    3, LocalTime.of(12, 30), DeliveryZone.FURNITURE_STREET, OrderStatus.CANCELLED),
                order(
                    4,
                    LocalTime.of(13, 0),
                    DeliveryZone.SHOPPING_CENTER,
                    OrderStatus.IN_PREPARATION)));

    DailyOrdersResponse response = serviceAt(LocalTime.of(11, 0)).getDailyOrders(null);

    assertThat(response.totalLunches()).isEqualTo(6);
    assertThat(response.orders()).hasSize(3);
  }

  @Test
  void cp14_ordersCanBeFilteredByZone() {
    when(orderRepository.findByOrderDateAndDeliveryZoneOrderByDeliveryTimeAsc(
            TODAY, DeliveryZone.FURNITURE_STREET))
        .thenReturn(
            List.of(
                order(5, LocalTime.of(12, 0), DeliveryZone.FURNITURE_STREET, OrderStatus.PENDING)));

    DailyOrdersResponse response =
        serviceAt(LocalTime.of(11, 0)).getDailyOrders(DeliveryZone.FURNITURE_STREET);

    assertThat(response.orders()).allMatch(o -> o.deliveryZone() == DeliveryZone.FURNITURE_STREET);
    assertThat(response.totalLunches()).isEqualTo(5);
  }

  // ---------- HU-04 ----------

  @Test
  void cp15b_validTransitionUpdatesStatus() {
    Order pending =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(pending));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    OrderResponse response =
        serviceAt(LocalTime.of(11, 0))
            .updateStatus(7L, OrderStatus.IN_PREPARATION, StaffRole.ADMIN, "staff");

    assertThat(response.status()).isEqualTo(OrderStatus.IN_PREPARATION);
  }

  @Test
  void cp15c_skippingAStateIsRejected() {
    Order pending =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(pending));
    OrderService service = serviceAt(LocalTime.of(11, 0));

    assertThatThrownBy(
            () -> service.updateStatus(7L, OrderStatus.DELIVERED, StaffRole.ADMIN, "staff"))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(pending.getStatus()).isEqualTo(OrderStatus.PENDING);
  }

  @Test
  void cp16_finalOrderCannotBeModified() {
    Order delivered =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.DELIVERED);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(delivered));
    OrderService service = serviceAt(LocalTime.of(11, 0));

    assertThatThrownBy(
            () -> service.updateStatus(7L, OrderStatus.PENDING, StaffRole.ADMIN, "staff"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("ya no se puede modificar");
  }

  @Test
  void cp16b_unknownOrderReturnsNotFound() {
    when(orderRepository.findById(99L)).thenReturn(Optional.empty());
    OrderService service = serviceAt(LocalTime.of(11, 0));

    assertThatThrownBy(
            () -> service.updateStatus(99L, OrderStatus.IN_PREPARATION, StaffRole.ADMIN, "staff"))
        .isInstanceOf(NotFoundException.class);
  }

  // ---------- Pago en línea ----------

  @Test
  void pay01_withPaymentsRequiredTheOrderWaitsForPayment() {
    dishExistsAndSaveWorks();
    when(paymentService.startPayment(any(Order.class)))
        .thenReturn(
            new PaymentGateway.PaymentSession("SBX-1", "http://localhost/pago?reference=SBX-1"));

    CreatedOrderResponse response =
        serviceAt(LocalTime.of(9, 0), PAYMENTS_ON).createOrder(request(2, LocalTime.of(12, 0)));

    assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    assertThat(response.paymentUrl()).isEqualTo("http://localhost/pago?reference=SBX-1");
    assertThat(response.total()).isEqualByComparingTo("24000");
    assertThat(response.accessCode()).hasSize(10);
    verify(confirmationService, never()).confirm(any(Order.class));
  }

  @Test
  void pay02_withoutPaymentsTheOrderIsConfirmedAtOnce() {
    dishExistsAndSaveWorks();

    CreatedOrderResponse response =
        serviceAt(LocalTime.of(9, 0)).createOrder(request(1, LocalTime.of(12, 0)));

    assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
    assertThat(response.paymentUrl()).isNull();
    verify(confirmationService).confirm(any(Order.class));
    verify(paymentService, never()).startPayment(any(Order.class));
  }

  // ---------- Límite de pedidos por día (valores límite: 99 y 100) ----------

  @Test
  void lim01_theHundredthOrderIsAccepted() {
    when(orderRepository.countByOrderDateAndStatusNotIn(any(), any())).thenReturn(99L);
    dishExistsAndSaveWorks();

    CreatedOrderResponse response =
        serviceAt(LocalTime.of(9, 0)).createOrder(request(1, LocalTime.of(12, 0)));

    assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
  }

  @Test
  void lim02_theOrderNumber101IsRejected() {
    when(orderRepository.countByOrderDateAndStatusNotIn(any(), any())).thenReturn(100L);
    OrderService service = serviceAt(LocalTime.of(9, 0));
    CreateOrderRequest request = request(1, LocalTime.of(12, 0));

    assertThatThrownBy(() -> service.createOrder(request))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Ya se alcanzó el máximo de 100 pedidos para hoy");
    verify(orderRepository, never()).save(any());
  }

  // ---------- Estados por rol ----------

  @Test
  void role01_kitchenMovesAPendingOrderToPreparationAndTakesTheTicket() {
    Order pending =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(pending));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    OrderResponse response =
        serviceAt(LocalTime.of(11, 0))
            .updateStatus(7L, OrderStatus.IN_PREPARATION, StaffRole.KITCHEN, "cocina1");

    assertThat(response.status()).isEqualTo(OrderStatus.IN_PREPARATION);
    verify(ticketService).acknowledge(pending, "cocina1");
    verify(notifier).orderChanged(any(OrderResponse.class));
  }

  @Test
  void role02b_kitchenMarksAnOrderInPreparationAsReady() {
    Order inPreparation =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.IN_PREPARATION);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(inPreparation));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    OrderResponse response =
        serviceAt(LocalTime.of(11, 0))
            .updateStatus(7L, OrderStatus.READY, StaffRole.KITCHEN, "cocina1");

    assertThat(response.status()).isEqualTo(OrderStatus.READY);
    verify(ticketService, never()).acknowledge(any(Order.class), any());
  }

  @Test
  void role02_deliveryMarksAReadyOrderAsDelivered() {
    Order ready = order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.READY);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(ready));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    OrderResponse response =
        serviceAt(LocalTime.of(11, 0))
            .updateStatus(7L, OrderStatus.DELIVERED, StaffRole.DELIVERY, "reparto1");

    assertThat(response.status()).isEqualTo(OrderStatus.DELIVERED);
    verify(ticketService, never()).acknowledge(any(Order.class), any());
  }

  @Test
  void role03_cashierCancelsAnUnpaidOrder() {
    Order unpaid =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING_PAYMENT);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(unpaid));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    OrderResponse response =
        serviceAt(LocalTime.of(11, 0))
            .updateStatus(7L, OrderStatus.CANCELLED, StaffRole.CASHIER, "caja1");

    assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
  }

  @Test
  void role04_aRoleCannotMakeAChangeThatIsNotItsJob() {
    Order pending =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(pending));
    OrderService service = serviceAt(LocalTime.of(11, 0));

    assertThatThrownBy(
            () -> service.updateStatus(7L, OrderStatus.CANCELLED, StaffRole.KITCHEN, "cocina1"))
        .isInstanceOf(ForbiddenActionException.class)
        .hasMessageContaining("KITCHEN");
    assertThat(pending.getStatus()).isEqualTo(OrderStatus.PENDING);
    verify(orderRepository, never()).save(any());
  }

  @Test
  void role05_anExpiredOrderCannotBeModified() {
    Order expired =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.EXPIRED);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(expired));
    OrderService service = serviceAt(LocalTime.of(11, 0));

    assertThatThrownBy(
            () -> service.updateStatus(7L, OrderStatus.PENDING, StaffRole.ADMIN, "staff"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("ya no se puede modificar");
  }

  // ---------- Totales y consulta del cliente ----------

  @Test
  void tot01_totalLunchesIgnoresUnpaidCancelledAndExpiredOrders() {
    when(orderRepository.findByOrderDateOrderByDeliveryTimeAsc(TODAY))
        .thenReturn(
            List.of(
                order(
                    5,
                    LocalTime.of(12, 0),
                    DeliveryZone.SHOPPING_CENTER,
                    OrderStatus.PENDING_PAYMENT),
                order(2, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING),
                order(7, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.EXPIRED),
                order(
                    3, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.CANCELLED)));

    DailyOrdersResponse response = serviceAt(LocalTime.of(11, 0)).getDailyOrders(null);

    assertThat(response.totalLunches()).isEqualTo(2);
  }

  @Test
  void trk01_theCustomerSeesTheOrderWithTheRightCode() {
    Order pending =
        order(2, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(pending));

    var tracking = serviceAt(LocalTime.of(11, 0)).track(7L, pending.getAccessCode());

    assertThat(tracking.status()).isEqualTo(OrderStatus.PENDING);
    assertThat(tracking.total()).isEqualByComparingTo("24000");
    assertThat(tracking.paymentPending()).isFalse();
  }

  @Test
  void trk02_aWrongCodeLooksLikeAnOrderThatDoesNotExist() {
    Order pending =
        order(2, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    ReflectionTestUtils.setField(pending, "id", 7L);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(pending));
    OrderService service = serviceAt(LocalTime.of(11, 0));

    assertThatThrownBy(() -> service.track(7L, "CODIGO-FALSO"))
        .isInstanceOf(NotFoundException.class);
  }

  // ---------- Aviso de llegada ----------

  @Test
  void arr01_theCustomerOfTheShoppingCenterCanAnnounceArrivalOnce() {
    Order reserved =
        order(2, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(reserved));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    var tracking = serviceAt(LocalTime.of(11, 45)).notifyArrival(7L, reserved.getAccessCode());

    assertThat(tracking.arrived()).isTrue();
    assertThat(reserved.getArrivedAt()).isEqualTo(LocalDateTime.of(TODAY, LocalTime.of(11, 45)));
    verify(notifier).orderChanged(any(OrderResponse.class));
  }

  @Test
  void arr02_anOrderInPreparationCanAlsoAnnounceArrival() {
    Order preparing =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.IN_PREPARATION);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(preparing));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    var tracking = serviceAt(LocalTime.of(11, 45)).notifyArrival(7L, preparing.getAccessCode());

    assertThat(tracking.arrived()).isTrue();
  }

  @Test
  void arr03_theFurnitureStreetCannotAnnounceArrival() {
    Order street =
        order(1, LocalTime.of(12, 0), DeliveryZone.FURNITURE_STREET, OrderStatus.PENDING);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(street));
    OrderService service = serviceAt(LocalTime.of(11, 45));
    String code = street.getAccessCode();

    assertThatThrownBy(() -> service.notifyArrival(7L, code))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("centro comercial");
    assertThat(street.getArrivedAt()).isNull();
    verify(notifier, never()).orderChanged(any());
  }

  @ParameterizedTest(name = "un pedido {0} no puede avisar llegada")
  @ValueSource(strings = {"PENDING_PAYMENT", "DELIVERED", "CANCELLED", "EXPIRED"})
  void arr04_onlyReservedOrPreparingOrdersCanAnnounceArrival(String status) {
    Order other =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.valueOf(status));
    when(orderRepository.findById(7L)).thenReturn(Optional.of(other));
    OrderService service = serviceAt(LocalTime.of(11, 45));
    String code = other.getAccessCode();

    assertThatThrownBy(() -> service.notifyArrival(7L, code))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("reservado");
  }

  @Test
  void arr05_theArrivalCanOnlyBeAnnouncedOnce() {
    Order reserved =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    reserved.setArrivedAt(LocalDateTime.of(TODAY, LocalTime.of(11, 40)));
    when(orderRepository.findById(7L)).thenReturn(Optional.of(reserved));
    OrderService service = serviceAt(LocalTime.of(11, 45));
    String code = reserved.getAccessCode();

    assertThatThrownBy(() -> service.notifyArrival(7L, code))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Ya avisaste");
    verify(notifier, never()).orderChanged(any());
  }

  @Test
  void arr06_aWrongCodeLooksLikeAnOrderThatDoesNotExist() {
    Order reserved =
        order(1, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(reserved));
    OrderService service = serviceAt(LocalTime.of(11, 45));

    assertThatThrownBy(() -> service.notifyArrival(7L, "CODIGO-FALSO"))
        .isInstanceOf(NotFoundException.class);
    assertThat(reserved.getArrivedAt()).isNull();
  }

  // ---------- Varios platos en un mismo pedido ----------

  private static CreateOrderRequest requestOf(OrderItemRequest... items) {
    return new CreateOrderRequest(
        null,
        null,
        List.of(items),
        "Valentina",
        LocalTime.of(12, 0),
        DeliveryZone.SHOPPING_CENTER,
        "Local 12");
  }

  private Dish soup() {
    return new Dish("Sopa del día", "Con pan", new BigDecimal("9000"), true);
  }

  @Test
  void multi01_oneOrderCanHaveSeveralDishes() {
    when(dishRepository.findById(1L)).thenReturn(Optional.of(availableDish));
    when(dishRepository.findById(2L)).thenReturn(Optional.of(soup()));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    CreatedOrderResponse response =
        serviceAt(LocalTime.of(9, 0))
            .createOrder(requestOf(new OrderItemRequest(1L, 2), new OrderItemRequest(2L, 1)));

    assertThat(response.quantity()).isEqualTo(3);
    assertThat(response.total()).isEqualByComparingTo("33000");
    assertThat(response.dishName()).isEqualTo("Pollo a la plancha + Sopa del día");
    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).quantity()).isEqualTo(2);
    assertThat(response.items().get(1).dishName()).isEqualTo("Sopa del día");
  }

  @Test
  void multi02_theSameDishTwiceIsMerged() {
    dishExistsAndSaveWorks();

    CreatedOrderResponse response =
        serviceAt(LocalTime.of(9, 0))
            .createOrder(requestOf(new OrderItemRequest(1L, 2), new OrderItemRequest(1L, 3)));

    assertThat(response.items()).hasSize(1);
    assertThat(response.quantity()).isEqualTo(5);
    assertThat(response.total()).isEqualByComparingTo("60000");
  }

  @Test
  void multi03_theLimitCountsAllDishesTogether() {
    OrderService service = serviceAt(LocalTime.of(9, 0));
    CreateOrderRequest tooMany =
        requestOf(new OrderItemRequest(1L, 6), new OrderItemRequest(2L, 5));

    assertThatThrownBy(() -> service.createOrder(tooMany))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cantidad");
    verify(orderRepository, never()).save(any());
  }

  @Test
  void multi04_everyDishNeedsAtLeastOneLunch() {
    OrderService service = serviceAt(LocalTime.of(9, 0));
    CreateOrderRequest negative =
        requestOf(new OrderItemRequest(1L, 3), new OrderItemRequest(2L, -1));

    assertThatThrownBy(() -> service.createOrder(negative))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("al menos 1");
    verify(orderRepository, never()).save(any());
  }

  @Test
  void multi05_anOrderWithoutDishesIsRejected() {
    OrderService service = serviceAt(LocalTime.of(9, 0));
    CreateOrderRequest empty =
        new CreateOrderRequest(
            null,
            null,
            null,
            "Valentina",
            LocalTime.of(12, 0),
            DeliveryZone.SHOPPING_CENTER,
            "Local 12");

    assertThatThrownBy(() -> service.createOrder(empty))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("al menos un plato");
  }

  @Test
  void multi06_oneUnavailableDishRejectsTheWholeOrder() {
    when(dishRepository.findById(1L)).thenReturn(Optional.of(availableDish));
    when(dishRepository.findById(2L))
        .thenReturn(Optional.of(new Dish("Pasta", "Agotada", new BigDecimal("11000"), false)));
    OrderService service = serviceAt(LocalTime.of(9, 0));
    CreateOrderRequest order = requestOf(new OrderItemRequest(1L, 1), new OrderItemRequest(2L, 1));

    assertThatThrownBy(() -> service.createOrder(order))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("no está disponible");
    verify(orderRepository, never()).save(any());
  }

  @Test
  void stock01_anOrderThatPassesTheDailyLimitIsRejected() {
    availableDish.setDailyLimit(10);
    when(dishRepository.findById(1L)).thenReturn(Optional.of(availableDish));
    when(orderRepository.soldQuantity(any(), any())).thenReturn(8L);
    OrderService service = serviceAt(LocalTime.of(9, 0));
    CreateOrderRequest order = requestOf(new OrderItemRequest(1L, 3));

    assertThatThrownBy(() -> service.createOrder(order))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("solo quedan 2");
    verify(orderRepository, never()).save(any());
  }

  @Test
  void stock02_aDishWithNothingLeftIsSoldOut() {
    availableDish.setDailyLimit(10);
    when(dishRepository.findById(1L)).thenReturn(Optional.of(availableDish));
    when(orderRepository.soldQuantity(any(), any())).thenReturn(10L);
    OrderService service = serviceAt(LocalTime.of(9, 0));
    CreateOrderRequest order = requestOf(new OrderItemRequest(1L, 1));

    assertThatThrownBy(() -> service.createOrder(order))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("agotado");
  }

  @Test
  void stock03_anOrderWithinTheRemainingStockIsAccepted() {
    availableDish.setDailyLimit(10);
    dishExistsAndSaveWorks();
    when(orderRepository.soldQuantity(any(), any())).thenReturn(7L);

    CreatedOrderResponse response =
        serviceAt(LocalTime.of(9, 0)).createOrder(requestOf(new OrderItemRequest(1L, 3)));

    assertThat(response.quantity()).isEqualTo(3);
  }

  @Test
  void multi07_anOldOrderWithoutItemsStillShowsItsDish() {
    Order old = order(2, LocalTime.of(12, 0), DeliveryZone.SHOPPING_CENTER, OrderStatus.PENDING);
    ReflectionTestUtils.setField(old, "items", new java.util.ArrayList<>());

    assertThat(old.lines()).hasSize(1);
    assertThat(old.lines().get(0).quantity()).isEqualTo(2);
    assertThat(old.dishSummary()).isEqualTo("Pollo a la plancha");
  }
}
