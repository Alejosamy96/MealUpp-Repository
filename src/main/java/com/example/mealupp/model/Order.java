package com.example.mealupp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

/** Pedido de almuerzo de un cliente. */
@Entity(name = "CustomerOrder")
@Table(name = "customer_orders")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Dish dish;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false)
  private String customerName;

  @Column(nullable = false)
  private LocalDate orderDate;

  @Column(nullable = false)
  private LocalTime deliveryTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeliveryZone deliveryZone;

  @Column(nullable = false)
  private String deliveryDetail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  protected Order() {}

  public Order(
      Dish dish,
      int quantity,
      String customerName,
      LocalDate orderDate,
      LocalTime deliveryTime,
      DeliveryZone deliveryZone,
      String deliveryDetail) {
    this.dish = dish;
    this.quantity = quantity;
    this.customerName = customerName;
    this.orderDate = orderDate;
    this.deliveryTime = deliveryTime;
    this.deliveryZone = deliveryZone;
    this.deliveryDetail = deliveryDetail;
    this.status = OrderStatus.PENDING;
  }

  public Long getId() {
    return id;
  }

  public Dish getDish() {
    return dish;
  }

  public int getQuantity() {
    return quantity;
  }

  public String getCustomerName() {
    return customerName;
  }

  public LocalDate getOrderDate() {
    return orderDate;
  }

  public LocalTime getDeliveryTime() {
    return deliveryTime;
  }

  public DeliveryZone getDeliveryZone() {
    return deliveryZone;
  }

  public String getDeliveryDetail() {
    return deliveryDetail;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }
}
