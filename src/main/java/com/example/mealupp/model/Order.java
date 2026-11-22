package com.example.mealupp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

  /**
   * Primer plato del pedido. Se conserva (junto con la cantidad total) para que las bases de datos
   * ya creadas sigan funcionando; el detalle completo está en "items".
   */
  @ManyToOne(optional = false)
  private Dish dish;

  /** Total de almuerzos del pedido (suma de todos los platos). */
  @Column(nullable = false)
  private int quantity;

  /** Platos del pedido. Los pedidos antiguos no tienen filas aquí: se leen de dish y quantity. */
  @OneToMany(
      mappedBy = "order",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("id ASC")
  private List<OrderItem> items = new ArrayList<>();

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

  /** Hora en que el cliente avisó que llegó a recoger (solo pedidos del centro comercial). */
  private LocalDateTime arrivedAt;

  protected Order() {}

  /** Pedido de un solo plato. */
  public Order(
      Dish dish,
      int quantity,
      String customerName,
      LocalDate orderDate,
      LocalTime deliveryTime,
      DeliveryZone deliveryZone,
      String deliveryDetail) {
    this(
        List.of(OrderLine.of(dish, quantity)),
        customerName,
        orderDate,
        deliveryTime,
        deliveryZone,
        deliveryDetail);
  }

  /** Pedido de uno o varios platos. */
  public Order(
      List<OrderLine> lines,
      String customerName,
      LocalDate orderDate,
      LocalTime deliveryTime,
      DeliveryZone deliveryZone,
      String deliveryDetail) {
    if (lines == null || lines.isEmpty()) {
      throw new IllegalArgumentException("Un pedido necesita al menos un plato");
    }
    OrderLine first = lines.get(0);
    this.dish = first.dish();
    this.quantity = lines.stream().mapToInt(OrderLine::quantity).sum();
    this.customerName = customerName;
    this.orderDate = orderDate;
    this.deliveryTime = deliveryTime;
    this.deliveryZone = deliveryZone;
    this.deliveryDetail = deliveryDetail;
    this.status = OrderStatus.PENDING;
    this.unitPrice = first.unitPrice();
    this.total = lines.stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    for (OrderLine line : lines) {
      this.items.add(new OrderItem(this, line));
    }
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

  /** Total de almuerzos del pedido. */
  public int getQuantity() {
    return quantity;
  }

  /** Platos del pedido con su cantidad y precio. En pedidos antiguos, el único plato guardado. */
  public List<OrderLine> lines() {
    if (items.isEmpty()) {
      return List.of(new OrderLine(dish, quantity, getUnitPrice()));
    }
    return items.stream()
        .map(item -> new OrderLine(item.getDish(), item.getQuantity(), item.getUnitPrice()))
        .toList();
  }

  /** Nombre para mostrar: el plato, o los platos unidos con "+" si son varios. */
  public String dishSummary() {
    return lines().stream().map(line -> line.dish().getName()).collect(Collectors.joining(" + "));
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

  public LocalDateTime getArrivedAt() {
    return arrivedAt;
  }

  public void setArrivedAt(LocalDateTime arrivedAt) {
    this.arrivedAt = arrivedAt;
  }
}
