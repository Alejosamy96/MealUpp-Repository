package com.example.mealupp.service;

import com.example.mealupp.config.OrderRulesProperties;
import com.example.mealupp.config.PaymentProperties;
import com.example.mealupp.dto.CreateOrderRequest;
import com.example.mealupp.dto.CreatedOrderResponse;
import com.example.mealupp.dto.DailyOrdersResponse;
import com.example.mealupp.dto.OrderResponse;
import com.example.mealupp.dto.OrderTrackingResponse;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.ForbiddenActionException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.integration.notification.KitchenNotifier;
import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Dish;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.DishRepository;
import com.example.mealupp.repository.OrderRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pedidos: crearlos, consultarlos y cambiar su estado. */
@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final DishRepository dishRepository;
  private final OrderRulesProperties rules;
  private final PaymentProperties paymentProperties;
  private final Clock clock;
  private final PaymentService paymentService;
  private final OrderConfirmationService confirmationService;
  private final KitchenTicketService ticketService;
  private final KitchenNotifier notifier;

  public OrderService(
      OrderRepository orderRepository,
      DishRepository dishRepository,
      OrderRulesProperties rules,
      PaymentProperties paymentProperties,
      Clock clock,
      PaymentService paymentService,
      OrderConfirmationService confirmationService,
      KitchenTicketService ticketService,
      KitchenNotifier notifier) {
    this.orderRepository = orderRepository;
    this.dishRepository = dishRepository;
    this.rules = rules;
    this.paymentProperties = paymentProperties;
    this.clock = clock;
    this.paymentService = paymentService;
    this.confirmationService = confirmationService;
    this.ticketService = ticketService;
    this.notifier = notifier;
  }

  // ---------- Crear un pedido ----------

  @Transactional
  public CreatedOrderResponse createOrder(CreateOrderRequest request) {
    LocalDateTime now = LocalDateTime.now(clock);
    validateCutoff(now.toLocalTime());
    validateQuantity(request.quantity());
    validateDeliveryTime(request.deliveryTime());
    validateDailyLimit(now.toLocalDate());
    Dish dish = findAvailableDish(request.dishId());

    Order order =
        new Order(
            dish,
            request.quantity(),
            request.customerName().trim(),
            now.toLocalDate(),
            request.deliveryTime(),
            request.deliveryZone(),
            request.deliveryDetail().trim());
    order.setCreatedAt(now);

    if (paymentProperties.required()) {
      order.setStatus(OrderStatus.PENDING_PAYMENT);
      Order saved = orderRepository.save(order);
      String paymentUrl = paymentService.startPayment(saved).paymentUrl();
      return CreatedOrderResponse.from(saved, paymentUrl);
    }
    Order saved = orderRepository.save(order);
    confirmationService.confirm(saved);
    return CreatedOrderResponse.from(saved, null);
  }

  private void validateCutoff(LocalTime now) {
    if (!now.isBefore(rules.cutoffTime())) {
      throw new BusinessRuleException(
          "Los pedidos del día se reciben antes de las " + rules.cutoffTime());
    }
  }

  private void validateQuantity(int quantity) {
    if (quantity < rules.minQuantity() || quantity > rules.maxQuantity()) {
      throw new BusinessRuleException(
          "La cantidad debe estar entre " + rules.minQuantity() + " y " + rules.maxQuantity());
    }
  }

  private void validateDeliveryTime(LocalTime deliveryTime) {
    if (deliveryTime.isBefore(rules.deliveryStart()) || deliveryTime.isAfter(rules.deliveryEnd())) {
      throw new BusinessRuleException(
          "La hora de entrega debe estar entre "
              + rules.deliveryStart()
              + " y "
              + rules.deliveryEnd());
    }
  }

  private void validateDailyLimit(LocalDate today) {
    long taken =
        orderRepository.countByOrderDateAndStatusNotIn(
            today, List.of(OrderStatus.CANCELLED, OrderStatus.EXPIRED));
    if (taken >= rules.maxPerDay()) {
      throw new BusinessRuleException(
          "Ya se alcanzó el máximo de " + rules.maxPerDay() + " pedidos para hoy");
    }
  }

  private Dish findAvailableDish(Long dishId) {
    Dish dish =
        dishRepository
            .findById(dishId)
            .orElseThrow(() -> new NotFoundException("El plato no existe"));
    if (!dish.isAvailable()) {
      throw new BusinessRuleException("El plato no está disponible hoy");
    }
    return dish;
  }

  // ---------- Consultar ----------

  @Transactional(readOnly = true)
  public DailyOrdersResponse getDailyOrders(DeliveryZone zone) {
    LocalDate today = LocalDate.now(clock);
    List<Order> orders =
        zone == null
            ? orderRepository.findByOrderDateOrderByDeliveryTimeAsc(today)
            : orderRepository.findByOrderDateAndDeliveryZoneOrderByDeliveryTimeAsc(today, zone);
    int totalLunches =
        orders.stream()
            .filter(o -> o.getStatus().countsForTotals())
            .mapToInt(Order::getQuantity)
            .sum();
    return new DailyOrdersResponse(
        today, totalLunches, orders.stream().map(OrderResponse::from).toList());
  }

  /** El cliente consulta su pedido con el código de acceso que recibió al crearlo. */
  @Transactional(readOnly = true)
  public OrderTrackingResponse track(Long orderId, String accessCode) {
    return orderRepository
        .findById(orderId)
        .filter(order -> PaymentService.codeMatches(order.getAccessCode(), accessCode))
        .map(OrderTrackingResponse::from)
        .orElseThrow(() -> new NotFoundException("El pedido no existe"));
  }

  // ---------- Aviso de llegada ----------

  /**
   * El cliente avisa que ya llegó a recoger. Solo vale para pedidos del centro comercial que están
   * reservados o en preparación, y se puede avisar una sola vez. Se avisa a cocina (comanda) y a
   * entrega, caja y administración (pedido).
   */
  @Transactional
  public OrderTrackingResponse notifyArrival(Long orderId, String accessCode) {
    Order order =
        orderRepository
            .findById(orderId)
            .filter(found -> PaymentService.codeMatches(found.getAccessCode(), accessCode))
            .orElseThrow(() -> new NotFoundException("El pedido no existe"));
    if (order.getDeliveryZone() != DeliveryZone.SHOPPING_CENTER) {
      throw new BusinessRuleException(
          "El aviso de llegada solo está disponible para pedidos del centro comercial");
    }
    if (order.getStatus() != OrderStatus.PENDING
        && order.getStatus() != OrderStatus.IN_PREPARATION) {
      throw new BusinessRuleException(
          "Solo se puede avisar la llegada de un pedido reservado o en preparación");
    }
    if (order.getArrivedAt() != null) {
      throw new BusinessRuleException("Ya avisaste tu llegada");
    }
    order.setArrivedAt(LocalDateTime.now(clock));
    Order saved = orderRepository.save(order);
    ticketService.viewFor(saved).ifPresent(notifier::ticketUpdated);
    notifier.orderChanged(OrderResponse.from(saved));
    return OrderTrackingResponse.from(saved);
  }

  // ---------- Cambiar el estado ----------

  /**
   * Cambia el estado de un pedido. Cada rol solo puede hacer los cambios que le corresponden:
   * cocina pasa a preparación, entrega marca como entregado y caja cancela.
   */
  @Transactional
  public OrderResponse updateStatus(
      Long orderId, OrderStatus newStatus, StaffRole role, String username) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new NotFoundException("El pedido no existe"));
    OrderStatus current = order.getStatus();
    if (current.isFinal()) {
      throw new BusinessRuleException("Un pedido " + current + " ya no se puede modificar");
    }
    if (!current.canTransitionTo(newStatus)) {
      throw new BusinessRuleException("No se puede pasar de " + current + " a " + newStatus);
    }
    if (!role.canMove(current, newStatus)) {
      throw new ForbiddenActionException(
          "El rol " + role + " no puede pasar un pedido de " + current + " a " + newStatus);
    }
    order.setStatus(newStatus);
    Order saved = orderRepository.save(order);
    if (newStatus == OrderStatus.IN_PREPARATION) {
      ticketService.acknowledge(saved, username);
    }
    ticketService.viewFor(saved).ifPresent(notifier::ticketUpdated);
    OrderResponse response = OrderResponse.from(saved);
    notifier.orderChanged(response);
    return response;
  }
}
