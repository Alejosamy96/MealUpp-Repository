package com.example.mealupp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;

/** Plato del menú de Ferxxos. */
@Entity
public class Dish {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  private String description;

  @Column(nullable = false)
  private BigDecimal price;

  @Column(nullable = false)
  private boolean available;

  /** Máximo de almuerzos de este plato que se venden por día. Nulo significa sin límite. */
  private Integer dailyLimit;

  /** Clave base de la foto en el almacenamiento (por ejemplo dishes/3/ab12cd). */
  private String imageKey;

  protected Dish() {}

  public Dish(String name, String description, BigDecimal price, boolean available) {
    this.name = name;
    this.description = description;
    this.price = price;
    this.available = available;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public Integer getDailyLimit() {
    return dailyLimit;
  }

  public void setDailyLimit(Integer dailyLimit) {
    this.dailyLimit = dailyLimit;
  }

  /** Cuántos almuerzos quedan hoy, dado lo ya vendido. Nulo si el plato no tiene límite. */
  public Integer remaining(long soldToday) {
    if (dailyLimit == null) {
      return null;
    }
    return (int) Math.max(0, dailyLimit - soldToday);
  }

  /** El plato está agotado si se marcó así a mano o si ya se vendió todo su cupo de hoy. */
  public boolean soldOut(long soldToday) {
    Integer left = remaining(soldToday);
    return !available || (left != null && left <= 0);
  }

  public String getImageKey() {
    return imageKey;
  }

  public void setImageKey(String imageKey) {
    this.imageKey = imageKey;
  }
}
