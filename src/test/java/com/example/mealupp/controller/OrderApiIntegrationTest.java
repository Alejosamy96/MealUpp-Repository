package com.example.mealupp.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.mealupp.model.Dish;
import com.example.mealupp.repository.DishRepository;
import com.example.mealupp.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Pruebas de integración: API + seguridad + base de datos H2. */
@SpringBootTest
@ActiveProfiles("test")
class OrderApiIntegrationTest {

  /** Fija la hora en 9:00 a. m. para que la regla de corte no dependa de cuándo se corra. */
  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock fixedClock() {
      ZoneId zone = ZoneId.of("America/Bogota");
      return Clock.fixed(LocalDateTime.of(2026, 10, 7, 9, 0).atZone(zone).toInstant(), zone);
    }
  }

  @Autowired private WebApplicationContext context;
  @Autowired private DishRepository dishRepository;
  @Autowired private OrderRepository orderRepository;

  private MockMvc mockMvc;
  private Long dishId;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    orderRepository.deleteAll();
    dishRepository.deleteAll();
    dishId =
        dishRepository
            .save(new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true))
            .getId();
    dishRepository.save(new Dish("Pasta", "Agotada", new BigDecimal("11000"), false));
  }

  private String orderJson(String zone) {
    return """
        {"dishId": %d, "quantity": 2, "customerName": "Valentina",
         "deliveryTime": "12:00", "deliveryZone": "%s", "deliveryDetail": "Local 12"}
        """
        .formatted(dishId, zone);
  }

  // ---------- HU-01 y HU-05: rutas públicas ----------

  @Test
  void cp01b_cp20_menuIsPublicAndOnlyShowsAvailableDishes() throws Exception {
    mockMvc
        .perform(get("/api/menu"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dishes.length()").value(1))
        .andExpect(jsonPath("$.dishes[0].name").value("Pollo a la plancha"));
  }

  // ---------- HU-02 ----------

  @Test
  void cp12b_cp20_validOrderIsCreatedWithoutLogin() throws Exception {
    mockMvc
        .perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson("SHOPPING_CENTER")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.deliveryZone").value("SHOPPING_CENTER"));
  }

  @Test
  void cp10_deliveryZoneOutsideCoverageIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson("OTRA")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cp10b_missingDeliveryDetailIsRejected() throws Exception {
    String json = orderJson("FURNITURE_STREET").replace("Local 12", " ");
    mockMvc
        .perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cp03b_quantityOutOfRangeReturnsBadRequestWithMessage() throws Exception {
    String json = orderJson("SHOPPING_CENTER").replace("\"quantity\": 2", "\"quantity\": 11");
    mockMvc
        .perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("La cantidad debe estar entre 1 y 10"));
  }

  // ---------- HU-05: control de acceso ----------

  @Test
  void cp17_staffEndpointsWithoutLoginReturn401() throws Exception {
    mockMvc.perform(get("/api/staff/orders")).andExpect(status().isUnauthorized());
  }

  @Test
  void cp18_userWithoutStaffRoleReturns403() throws Exception {
    mockMvc
        .perform(get("/api/staff/orders").with(user("cliente").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void cp18b_wrongPasswordReturns401() throws Exception {
    mockMvc
        .perform(get("/api/staff/orders").with(httpBasic("staff", "incorrecta")))
        .andExpect(status().isUnauthorized());
  }

  // ---------- HU-03 y HU-04 de punta a punta con rol STAFF ----------

  @Test
  void cp19_staffSeesOrdersAndUpdatesStatus() throws Exception {
    mockMvc.perform(
        post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderJson("SHOPPING_CENTER")));
    Long orderId = orderRepository.findAll().get(0).getId();

    mockMvc
        .perform(get("/api/staff/orders").with(httpBasic("staff", "test-password")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalLunches").value(2))
        .andExpect(jsonPath("$.orders.length()").value(1));

    mockMvc
        .perform(
            patch("/api/staff/orders/{id}/status", orderId)
                .with(httpBasic("staff", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"IN_PREPARATION\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PREPARATION"));

    mockMvc
        .perform(
            patch("/api/staff/orders/{id}/status", orderId)
                .with(httpBasic("staff", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"PENDING\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cp16c_unknownOrderReturns404() throws Exception {
    mockMvc
        .perform(
            patch("/api/staff/orders/{id}/status", 9999)
                .with(httpBasic("staff", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"IN_PREPARATION\"}"))
        .andExpect(status().isNotFound());
  }
}
