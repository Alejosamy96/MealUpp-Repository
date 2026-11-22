package com.example.mealupp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Un plato dentro de un pedido, con su cantidad y el precio de ese momento. */
@Entity
@Table(name = "order_items")
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "order_id")
  private Order order;

  @ManyToOne(optional = false)
  private Dish dish;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false)
  private BigDecimal unitPrice;

  protected OrderItem() {}

  OrderItem(Order order, OrderLine line) {
    this.order = order;
    this.dish = line.dish();
    this.quantity = line.quantity();
    this.unitPrice = line.unitPrice();
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

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }
}
