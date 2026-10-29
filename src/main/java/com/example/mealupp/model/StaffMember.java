package com.example.mealupp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Persona del restaurante registrada para entrar a las funciones internas (HU-05). */
@Entity
@Table(name = "staff_members")
public class StaffMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String fullName;

  /** Nunca se guarda la contraseña: solo su hash. */
  @Column(nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StaffRole role;

  @Column(nullable = false)
  private boolean active;

  protected StaffMember() {}

  public StaffMember(String username, String fullName, String passwordHash, StaffRole role) {
    this.username = username;
    this.fullName = fullName;
    this.passwordHash = passwordHash;
    this.role = role;
    this.active = true;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getFullName() {
    return fullName;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public StaffRole getRole() {
    return role;
  }

  public void setRole(StaffRole role) {
    this.role = role;
  }

  public boolean isActive() {
    return active;
  }
}
