package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mealupp.dto.KitchenTicketView;
import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Dish;
import com.example.mealupp.model.KitchenTicket;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.model.TicketStatus;
import com.example.mealupp.repository.KitchenTicketRepository;
import com.example.mealupp.repository.StaffMemberRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KitchenTicketServiceTest {

  private static final ZoneId ZONE = ZoneId.of("America/Bogota");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 9, 0);

  @Mock private KitchenTicketRepository ticketRepository;
  @Mock private StaffMemberRepository staffRepository;

  private KitchenTicketService service;
  private Order order;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
    service = new KitchenTicketService(ticketRepository, staffRepository, clock);
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
    ReflectionTestUtils.setField(order, "id", 7L);
  }

  @Test
  void createSavesANewTicketSentNow() {
    when(ticketRepository.findByCustomerOrderId(7L)).thenReturn(Optional.empty());
    when(ticketRepository.save(any(KitchenTicket.class))).thenAnswer(inv -> inv.getArgument(0));

    KitchenTicket ticket = service.create(order);

    assertThat(ticket.getStatus()).isEqualTo(TicketStatus.SENT);
    assertThat(ticket.getSentAt()).isEqualTo(NOW);
  }

  @Test
  void createDoesNotDuplicateAnExistingTicket() {
    KitchenTicket existing = new KitchenTicket(order, NOW);
    when(ticketRepository.findByCustomerOrderId(7L)).thenReturn(Optional.of(existing));

    assertThat(service.create(order)).isSameAs(existing);
    verify(ticketRepository, never()).save(any());
  }

  @Test
  void pendingListsTicketsOfOrdersStillInTheKitchen() {
    Order cooking =
        new Order(
            order.getDish(),
            1,
            "Marta",
            LocalDate.of(2026, 11, 4),
            LocalTime.of(13, 0),
            DeliveryZone.FURNITURE_STREET,
            "Local 3");
    cooking.setStatus(OrderStatus.IN_PREPARATION);
    when(ticketRepository.findByCustomerOrderStatusInOrderBySentAtAsc(
            List.of(OrderStatus.PENDING, OrderStatus.IN_PREPARATION)))
        .thenReturn(List.of(new KitchenTicket(order, NOW), new KitchenTicket(cooking, NOW)));

    List<KitchenTicketView> pending = service.pending();

    assertThat(pending).hasSize(2);
    assertThat(pending.get(0).dishName()).isEqualTo("Pollo a la plancha");
    assertThat(pending.get(0).quantity()).isEqualTo(2);
    assertThat(pending.get(1).orderStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
  }

  @Test
  void acknowledgeRecordsWhoTookTheTicketAndWhen() {
    KitchenTicket ticket = new KitchenTicket(order, NOW.minusMinutes(5));
    StaffMember cook = new StaffMember("cocina1", "Carlos", "hash", StaffRole.KITCHEN);
    when(ticketRepository.findByCustomerOrderId(7L)).thenReturn(Optional.of(ticket));
    when(staffRepository.findByUsername("cocina1")).thenReturn(Optional.of(cook));

    service.acknowledge(order, "cocina1");

    assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ACKNOWLEDGED);
    assertThat(ticket.getAcknowledgedBy()).isSameAs(cook);
    assertThat(ticket.getAcknowledgedAt()).isEqualTo(NOW);
    verify(ticketRepository).save(ticket);
  }

  @Test
  void acknowledgeTwiceKeepsTheFirstPerson() {
    KitchenTicket ticket = new KitchenTicket(order, NOW);
    ticket.acknowledge(new StaffMember("cocina1", "Carlos", "hash", StaffRole.KITCHEN), NOW);
    when(ticketRepository.findByCustomerOrderId(7L)).thenReturn(Optional.of(ticket));

    service.acknowledge(order, "cocina2");

    assertThat(ticket.getAcknowledgedBy().getUsername()).isEqualTo("cocina1");
    verify(ticketRepository, never()).save(any());
  }

  @Test
  void acknowledgeOfAnOrderWithoutTicketDoesNothing() {
    when(ticketRepository.findByCustomerOrderId(7L)).thenReturn(Optional.empty());

    service.acknowledge(order, "cocina1");

    verify(ticketRepository, never()).save(any());
  }

  @Test
  void viewForReturnsTheTicketOfTheOrder() {
    when(ticketRepository.findByCustomerOrderId(7L))
        .thenReturn(Optional.of(new KitchenTicket(order, NOW)));

    assertThat(service.viewFor(order)).isPresent();
  }
}
