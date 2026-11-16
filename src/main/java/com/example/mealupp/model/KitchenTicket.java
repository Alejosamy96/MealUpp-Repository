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
import java.time.LocalDateTime;

/** Comanda: el aviso que recibe la cocina cuando un pedido queda confirmado. */
@Entity
@Table(name = "kitchen_tickets")
public class KitchenTicket {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Order customerOrder;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TicketStatus status;

  @Column(nullable = false)
  private LocalDateTime sentAt;

  /** Persona de cocina que la tomó. */
  @ManyToOne private StaffMember acknowledgedBy;

  private LocalDateTime acknowledgedAt;

  protected KitchenTicket() {}

  public KitchenTicket(Order customerOrder, LocalDateTime sentAt) {
    this.customerOrder = customerOrder;
    this.status = TicketStatus.SENT;
    this.sentAt = sentAt;
  }

  public void acknowledge(StaffMember by, LocalDateTime at) {
    this.status = TicketStatus.ACKNOWLEDGED;
    this.acknowledgedBy = by;
    this.acknowledgedAt = at;
  }

  public Long getId() {
    return id;
  }

  public Order getCustomerOrder() {
    return customerOrder;
  }

  public TicketStatus getStatus() {
    return status;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }

  public StaffMember getAcknowledgedBy() {
    return acknowledgedBy;
  }

  public LocalDateTime getAcknowledgedAt() {
    return acknowledgedAt;
  }
}
