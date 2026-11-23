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

  /** Almuerzos de un plato vendidos en un día, según los platos de cada pedido. */
  @Query(
      "select coalesce(sum(i.quantity), 0L) from OrderItem i where i.dish.id = :dishId"
          + " and i.order.orderDate = :date and i.order.status not in :statuses")
  long soldInItems(
      @Param("dishId") Long dishId,
      @Param("date") LocalDate date,
      @Param("statuses") Collection<OrderStatus> statuses);

  /** Lo mismo para los pedidos antiguos, que no tienen platos aparte y usan solo "dish". */
  @Query(
      "select coalesce(sum(o.quantity), 0L) from CustomerOrder o where o.dish.id = :dishId"
          + " and o.orderDate = :date and o.status not in :statuses and o.items is empty")
  long soldInLegacyOrders(
      @Param("dishId") Long dishId,
      @Param("date") LocalDate date,
      @Param("statuses") Collection<OrderStatus> statuses);

  /** Almuerzos de un plato que ya se vendieron en el día (los cancelados y vencidos no cuentan). */
  default long soldQuantity(Long dishId, LocalDate date) {
    List<OrderStatus> excluded = List.of(OrderStatus.CANCELLED, OrderStatus.EXPIRED);
    return soldInItems(dishId, date, excluded) + soldInLegacyOrders(dishId, date, excluded);
  }

  /** Indica si algún pedido incluye el plato (como primer plato o como uno de sus platos). */
  @Query(
      "select count(o) > 0 from CustomerOrder o where o.dish.id = :dishId"
          + " or exists (select 1 from OrderItem i where i.order = o and i.dish.id = :dishId)")
  boolean existsByDishId(@Param("dishId") Long dishId);
}
