package com.example.mealupp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Dish;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.DishRepository;
import com.example.mealupp.repository.OrderRepository;
import com.example.mealupp.repository.StaffMemberRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** HU-06 de punta a punta: cada rol ve lo suyo y recibe un mensaje si entra a lo de otro. */
@SpringBootTest
@ActiveProfiles("test")
class RoleViewsApiIntegrationTest {

  private static final ZoneId ZONE = ZoneId.of("America/Bogota");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
  private static final String DENIED = "No tienes permiso para entrar a esta sección";

  /** Fija el día para que los pedidos de prueba sean "de hoy" sin importar cuándo se corra. */
  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(
          LocalDateTime.of(TODAY, LocalTime.of(9, 0)).atZone(ZONE).toInstant(), ZONE);
    }
  }

  @Autowired private WebApplicationContext context;
  @Autowired private StaffMemberRepository staffRepository;
  @Autowired private DishRepository dishRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private PasswordEncoder encoder;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    staffRepository.findAll().stream()
        .filter(m -> m.getRole() != StaffRole.ADMIN)
        .forEach(staffRepository::delete);
    if (staffRepository.findByUsername("staff").isEmpty()) {
      staffRepository.save(
          new StaffMember(
              "staff", "Administradora", encoder.encode("test-password"), StaffRole.ADMIN));
    }
    staffRepository.save(
        new StaffMember("cocina1", "Carlos", encoder.encode("clave-1234"), StaffRole.KITCHEN));
    staffRepository.save(
        new StaffMember("caja1", "Marta", encoder.encode("clave-1234"), StaffRole.CASHIER));

    orderRepository.deleteAll();
    dishRepository.deleteAll();
    Dish dish =
        dishRepository.save(
            new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true));
    orderRepository.save(
        new Order(
            dish,
            2,
            "Valentina",
            TODAY,
            LocalTime.of(12, 0),
            DeliveryZone.SHOPPING_CENTER,
            "Local 12"));
  }

  private String login(String username, String password) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\": \"%s\", \"password\": \"%s\"}".formatted(username, password)))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String token(String username, String password) throws Exception {
    return "Bearer " + JsonPath.read(login(username, password), "$.token");
  }

  @Test
  void loginTellsTheScreenWhichOptionsToShowForTheRole() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"cocina1\", \"password\": \"clave-1234\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("KITCHEN"))
        .andExpect(jsonPath("$.sections.length()").value(1))
        .andExpect(jsonPath("$.sections[0]").value("cocina"));
  }

  @Test
  void kitchenSeesItsOrdersWithoutCustomerData() throws Exception {
    mockMvc
        .perform(
            get("/api/staff/kitchen/orders")
                .header("Authorization", token("cocina1", "clave-1234")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].dishName").value("Pollo a la plancha"))
        .andExpect(jsonPath("$[0].customerName").doesNotExist());
  }

  @Test
  void cashierSeesItsOrdersWithTheTotal() throws Exception {
    mockMvc
        .perform(
            get("/api/staff/cashier/orders").header("Authorization", token("caja1", "clave-1234")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].total").value(24000))
        .andExpect(jsonPath("$[0].deliveryDetail").doesNotExist());
  }

  @Test
  void kitchenOpeningTheCashiersAddressGetsAMessage() throws Exception {
    mockMvc
        .perform(
            get("/api/staff/cashier/orders")
                .header("Authorization", token("cocina1", "clave-1234")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(DENIED));
  }

  @Test
  void cashierOpeningTheKitchenAddressGetsAMessage() throws Exception {
    mockMvc
        .perform(
            get("/api/staff/kitchen/orders").header("Authorization", token("caja1", "clave-1234")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(DENIED));
  }

  @Test
  void kitchenCannotOpenTheAdminScreens() throws Exception {
    String kitchen = token("cocina1", "clave-1234");

    mockMvc
        .perform(get("/api/staff/members").header("Authorization", kitchen))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(DENIED));
    mockMvc
        .perform(get("/api/staff/orders").header("Authorization", kitchen))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanOpenBothViews() throws Exception {
    String admin = token("staff", "test-password");

    mockMvc
        .perform(get("/api/staff/kitchen/orders").header("Authorization", admin))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/staff/cashier/orders").header("Authorization", admin))
        .andExpect(status().isOk());
  }

  @Test
  void withoutLoginTheViewsAnswer401() throws Exception {
    mockMvc.perform(get("/api/staff/kitchen/orders")).andExpect(status().isUnauthorized());
  }
}
