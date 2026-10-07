package com.example.mealupp.service;

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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-02, HU-03 y HU-04: registrar, consultar y actualizar pedidos. */
@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final DishRepository dishRepository;
  private final OrderRulesProperties rules;
  private final Clock clock;

  public OrderService(
      OrderRepository orderRepository,
      DishRepository dishRepository,
      OrderRulesProperties rules,
      Clock clock) {
    this.orderRepository = orderRepository;
    this.dishRepository = dishRepository;
    this.rules = rules;
    this.clock = clock;
  }

  // ---------- HU-02 ----------

  @Transactional
  public OrderResponse createOrder(CreateOrderRequest request) {
    LocalDateTime now = LocalDateTime.now(clock);
    validateCutoff(now.toLocalTime());
    validateQuantity(request.quantity());
    validateDeliveryTime(request.deliveryTime());
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
    return OrderResponse.from(orderRepository.save(order));
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

  // ---------- HU-03 ----------

  @Transactional(readOnly = true)
  public DailyOrdersResponse getDailyOrders(DeliveryZone zone) {
    LocalDate today = LocalDate.now(clock);
    List<Order> orders =
        zone == null
            ? orderRepository.findByOrderDateOrderByDeliveryTimeAsc(today)
            : orderRepository.findByOrderDateAndDeliveryZoneOrderByDeliveryTimeAsc(today, zone);
    int totalLunches =
        orders.stream()
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
            .mapToInt(Order::getQuantity)
            .sum();
    return new DailyOrdersResponse(
        today, totalLunches, orders.stream().map(OrderResponse::from).toList());
  }

  // ---------- HU-04 ----------

  @Transactional
  public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
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
    order.setStatus(newStatus);
    return OrderResponse.from(orderRepository.save(order));
  }
}
