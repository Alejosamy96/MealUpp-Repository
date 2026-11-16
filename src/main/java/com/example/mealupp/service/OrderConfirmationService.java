package com.example.mealupp.service;

import com.example.mealupp.dto.KitchenTicketView;
import com.example.mealupp.dto.OrderResponse;
import com.example.mealupp.integration.notification.KitchenNotifier;
import com.example.mealupp.model.KitchenTicket;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Qué pasa cuando un pedido queda confirmado: se crea la comanda para cocina y se avisa a las
 * pantallas. Se usa tanto cuando la pasarela aprueba el pago como cuando el pago en línea está
 * desactivado.
 */
@Service
public class OrderConfirmationService {

  private final OrderRepository orderRepository;
  private final KitchenTicketService ticketService;
  private final KitchenNotifier notifier;

  public OrderConfirmationService(
      OrderRepository orderRepository,
      KitchenTicketService ticketService,
      KitchenNotifier notifier) {
    this.orderRepository = orderRepository;
    this.ticketService = ticketService;
    this.notifier = notifier;
  }

  @Transactional
  public void confirm(Order order) {
    if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
      order.setStatus(OrderStatus.PENDING);
    }
    Order saved = orderRepository.save(order);
    KitchenTicket ticket = ticketService.create(saved);
    notifier.ticketCreated(KitchenTicketView.from(ticket));
    notifier.orderChanged(OrderResponse.from(saved));
  }
}
