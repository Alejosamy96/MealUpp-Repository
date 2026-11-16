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
import java.time.LocalDateTime;

/** Un intento de pago de un pedido. Un pedido puede tener varios si el primero falla. */
@Entity
@Table(name = "payments")
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Order customerOrder;

  @Column(nullable = false)
  private String provider;

  /** Referencia de la transacción en la pasarela. Es única. */
  @Column(unique = true)
  private String providerReference;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  protected Payment() {}

  public Payment(
      Order customerOrder, String provider, BigDecimal amount, String currency, LocalDateTime now) {
    this.customerOrder = customerOrder;
    this.provider = provider;
    this.amount = amount;
    this.currency = currency;
    this.status = PaymentStatus.PENDING;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public Order getCustomerOrder() {
    return customerOrder;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderReference() {
    return providerReference;
  }

  public void setProviderReference(String providerReference) {
    this.providerReference = providerReference;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public void setStatus(PaymentStatus status, LocalDateTime now) {
    this.status = status;
    this.updatedAt = now;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
