package com.example.mealupp.repository;

import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

  List<Order> findByOrderDateOrderByDeliveryTimeAsc(LocalDate orderDate);

  List<Order> findByOrderDateAndDeliveryZoneOrderByDeliveryTimeAsc(
      LocalDate orderDate, DeliveryZone deliveryZone);

  /** Cuenta los pedidos del día, sin los estados que se indiquen (por ejemplo los cancelados). */
  long countByOrderDateAndStatusNotIn(LocalDate orderDate, Collection<OrderStatus> statuses);

  List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime limit);

  /** Indica si algún pedido incluye el plato (como primer plato o como uno de sus platos). */
  @Query(
      "select count(o) > 0 from CustomerOrder o where o.dish.id = :dishId"
          + " or exists (select 1 from OrderItem i where i.order = o and i.dish.id = :dishId)")
  boolean existsByDishId(@Param("dishId") Long dishId);
}
