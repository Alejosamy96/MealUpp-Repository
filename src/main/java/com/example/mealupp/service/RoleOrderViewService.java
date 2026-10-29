package com.example.mealupp.service;

import com.example.mealupp.dto.CashierOrderView;
import com.example.mealupp.dto.KitchenOrderView;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.repository.OrderRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-06 (criterio 2): cada rol ve solo los pedidos que necesita. Está separado de OrderService para
 * no seguir subiendo su complejidad.
 */
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

  /** Caja: todos los pedidos de hoy con su valor. */
  @Transactional(readOnly = true)
  public List<CashierOrderView> cashierOrders() {
    return todayOrders().stream().map(CashierOrderView::from).toList();
  }

  private List<Order> todayOrders() {
    return orderRepository.findByOrderDateOrderByDeliveryTimeAsc(LocalDate.now(clock));
  }
}
