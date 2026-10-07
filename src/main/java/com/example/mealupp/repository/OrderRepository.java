package com.example.mealupp.repository;

import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Order;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

  List<Order> findByOrderDateOrderByDeliveryTimeAsc(LocalDate orderDate);

  List<Order> findByOrderDateAndDeliveryZoneOrderByDeliveryTimeAsc(
      LocalDate orderDate, DeliveryZone deliveryZone);
}
