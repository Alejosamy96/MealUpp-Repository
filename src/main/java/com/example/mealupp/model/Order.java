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
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Pedido de almuerzo de un cliente. */
@Entity(name = "CustomerOrder")
@Table(name = "customer_orders")
public class Order {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int CODE_LENGTH = 10;

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

  /** Precio del plato en el momento del pedido: no cambia si después cambia el precio. */
  private BigDecimal unitPrice;

  private BigDecimal total;

  /** Código que el cliente guarda para consultar su pedido sin iniciar sesión. */
  private String accessCode;

  private LocalDateTime createdAt;

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
    this.unitPrice = dish.getPrice() == null ? BigDecimal.ZERO : dish.getPrice();
    this.total = unitPrice.multiply(BigDecimal.valueOf(quantity));
    this.accessCode = newAccessCode();
  }

  private static String newAccessCode() {
    StringBuilder sb = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
    }
    return sb.toString();
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

  public BigDecimal getUnitPrice() {
    return unitPrice != null ? unitPrice : dish.getPrice();
  }

  /** Valor del pedido. En pedidos antiguos, sin valor guardado, se calcula con el precio actual. */
  public BigDecimal getTotal() {
    if (total != null) {
      return total;
    }
    BigDecimal price = dish.getPrice() == null ? BigDecimal.ZERO : dish.getPrice();
    return price.multiply(BigDecimal.valueOf(quantity));
  }

  public String getAccessCode() {
    return accessCode;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
