package com.example.mealupp.service;

import com.example.mealupp.dto.KitchenTicketView;
import com.example.mealupp.model.KitchenTicket;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.model.TicketStatus;
import com.example.mealupp.repository.KitchenTicketRepository;
import com.example.mealupp.repository.StaffMemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comandas a cocina: se crean cuando un pedido queda confirmado y se guardan en la base de datos.
 */
@Service
public class KitchenTicketService {

  private final KitchenTicketRepository ticketRepository;
  private final StaffMemberRepository staffRepository;
  private final Clock clock;

  public KitchenTicketService(
      KitchenTicketRepository ticketRepository,
      StaffMemberRepository staffRepository,
      Clock clock) {
    this.ticketRepository = ticketRepository;
    this.staffRepository = staffRepository;
    this.clock = clock;
  }

  /** Crea la comanda del pedido. Si ya existe, devuelve la misma (no se duplica). */
  @Transactional
  public KitchenTicket create(Order order) {
    return ticketRepository
        .findByCustomerOrderId(order.getId())
        .orElseGet(() -> ticketRepository.save(new KitchenTicket(order, LocalDateTime.now(clock))));
  }

  /**
   * Comandas que siguen en cocina: pedidos reservados (por empezar) o en preparación (por dejar
   * listos), de las más antiguas a las más nuevas.
   */
  @Transactional(readOnly = true)
  public List<KitchenTicketView> pending() {
    return ticketRepository
        .findByCustomerOrderStatusInOrderBySentAtAsc(
            List.of(OrderStatus.PENDING, OrderStatus.IN_PREPARATION))
        .stream()
        .map(KitchenTicketView::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<KitchenTicketView> viewFor(Order order) {
    return ticketRepository.findByCustomerOrderId(order.getId()).map(KitchenTicketView::from);
  }

  /** Marca la comanda como tomada por la persona de cocina que la pasó a preparación. */
  @Transactional
  public void acknowledge(Order order, String username) {
    ticketRepository
        .findByCustomerOrderId(order.getId())
        .filter(ticket -> ticket.getStatus() == TicketStatus.SENT)
        .ifPresent(
            ticket -> {
              ticket.acknowledge(
                  username == null ? null : staffRepository.findByUsername(username).orElse(null),
                  LocalDateTime.now(clock));
              ticketRepository.save(ticket);
            });
  }
}
