package com.example.mealupp.service;

import com.example.mealupp.dto.CashierOrderView;
import com.example.mealupp.dto.DeliveryOrderView;
import com.example.mealupp.dto.KitchenOrderView;
import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.repository.OrderRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cada rol ve solo los pedidos y los datos que necesita. */
@Service
public class RoleOrderViewService {

  private final OrderRepository orderRepository;
  private final Clock clock;

  public RoleOrderViewService(OrderRepository orderRepository, Clock clock) {
    this.orderRepository = orderRepository;
    this.clock = clock;
  }

  /** Cocina: pedidos de hoy que faltan por preparar o están en preparación. */
  @Transactional(readOnly = true)
  public List<KitchenOrderView> kitchenOrders() {
    return todayOrders().stream()
        .filter(
            o ->
                o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.IN_PREPARATION)
        .map(KitchenOrderView::from)
        .toList();
  }

  /** Cocina, pedidos ya hechos: los de hoy que se marcaron listos o ya se entregaron. */
  @Transactional(readOnly = true)
  public List<KitchenOrderView> kitchenDoneOrders() {
    return todayOrders().stream()
        .filter(o -> o.getStatus() == OrderStatus.READY || o.getStatus() == OrderStatus.DELIVERED)
        .map(KitchenOrderView::from)
        .toList();
  }

  /** Caja: todos los pedidos de hoy con su valor. */
  @Transactional(readOnly = true)
  public List<CashierOrderView> cashierOrders() {
    return todayOrders().stream().map(CashierOrderView::from).toList();
  }

  /** Entrega: pedidos de hoy que cocina ya dejó listos, opcionalmente de una zona. */
  @Transactional(readOnly = true)
  public List<DeliveryOrderView> deliveryOrders(DeliveryZone zone) {
    return deliveryOrdersWithStatus(zone, OrderStatus.READY);
  }

  /** Entrega, pedidos ya entregados hoy (con los datos del cliente y del pedido). */
  @Transactional(readOnly = true)
  public List<DeliveryOrderView> deliveredOrders(DeliveryZone zone) {
    return deliveryOrdersWithStatus(zone, OrderStatus.DELIVERED);
  }

  private List<DeliveryOrderView> deliveryOrdersWithStatus(DeliveryZone zone, OrderStatus status) {
    List<Order> orders =
        zone == null
            ? todayOrders()
            : orderRepository.findByOrderDateAndDeliveryZoneOrderByDeliveryTimeAsc(
                LocalDate.now(clock), zone);
    return orders.stream()
        .filter(o -> o.getStatus() == status)
        .map(DeliveryOrderView::from)
        .toList();
  }

  private List<Order> todayOrders() {
    return orderRepository.findByOrderDateOrderByDeliveryTimeAsc(LocalDate.now(clock));
  }
}
