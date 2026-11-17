package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mealupp.dto.KitchenTicketView;
import com.example.mealupp.dto.OrderResponse;
import com.example.mealupp.integration.notification.KitchenNotifier;
import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Dish;
import com.example.mealupp.model.KitchenTicket;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderConfirmationServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private KitchenTicketService ticketService;
  @Mock private KitchenNotifier notifier;

  private OrderConfirmationService service;
  private Order order;

  @BeforeEach
  void setUp() {
    service = new OrderConfirmationService(orderRepository, ticketService, notifier);
    Dish dish = new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true);
    order =
        new Order(
            dish,
            2,
            "Valentina",
            LocalDate.of(2026, 11, 4),
            LocalTime.of(12, 0),
            DeliveryZone.SHOPPING_CENTER,
            "Local 12");
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    when(ticketService.create(any(Order.class)))
        .thenAnswer(
            inv -> new KitchenTicket(inv.getArgument(0), LocalDateTime.of(2026, 11, 4, 9, 0)));
  }

  @Test
  void aPaidOrderBecomesPendingAndTheKitchenIsNotified() {
    order.setStatus(OrderStatus.PENDING_PAYMENT);

    service.confirm(order);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    verify(ticketService).create(order);
    verify(notifier).ticketCreated(any(KitchenTicketView.class));
    verify(notifier).orderChanged(any(OrderResponse.class));
  }

  @Test
  void anOrderThatIsAlreadyPendingKeepsItsStatus() {
    service.confirm(order);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    verify(notifier).ticketCreated(any(KitchenTicketView.class));
  }
}
