package com.example.mealupp.repository;

import com.example.mealupp.model.KitchenTicket;
import com.example.mealupp.model.TicketStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, Long> {

  List<KitchenTicket> findByStatusOrderBySentAtAsc(TicketStatus status);

  Optional<KitchenTicket> findByCustomerOrderId(Long orderId);
}
