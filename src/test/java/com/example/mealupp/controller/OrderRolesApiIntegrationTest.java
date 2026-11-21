package com.example.mealupp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.mealupp.model.Dish;
import com.example.mealupp.model.KitchenTicket;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.model.TicketStatus;
import com.example.mealupp.repository.DishRepository;
import com.example.mealupp.repository.KitchenTicketRepository;
import com.example.mealupp.repository.OrderRepository;
import com.example.mealupp.repository.PaymentRepository;
import com.example.mealupp.repository.StaffMemberRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
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

/** El recorrido de un pedido por cocina, entrega y caja: cada rol hace solo lo suyo. */
@SpringBootTest
@ActiveProfiles("test")
class OrderRolesApiIntegrationTest {

  /** Fija la hora en 9:00 a. m. del 4 de noviembre de 2026. */
  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock fixedClock() {
      ZoneId zone = ZoneId.of("America/Bogota");
      return Clock.fixed(LocalDateTime.of(2026, 11, 4, 9, 0).atZone(zone).toInstant(), zone);
    }
  }

  @Autowired private WebApplicationContext context;
  @Autowired private KitchenTicketRepository ticketRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private DishRepository dishRepository;
  @Autowired private StaffMemberRepository staffRepository;
  @Autowired private PasswordEncoder encoder;

  private MockMvc mockMvc;
  private Long dishId;

  @BeforeEach
  void setUp() {
    clean();
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    if (staffRepository.findByUsername("staff").isEmpty()) {
      staffRepository.save(
          new StaffMember(
              "staff", "Administradora", encoder.encode("test-password"), StaffRole.ADMIN));
    }
    staffRepository.save(
        new StaffMember("cocina1", "Carlos", encoder.encode("clave-1234"), StaffRole.KITCHEN));
    staffRepository.save(
        new StaffMember("reparto1", "Diego", encoder.encode("clave-1234"), StaffRole.DELIVERY));
    staffRepository.save(
        new StaffMember("caja1", "Marta", encoder.encode("clave-1234"), StaffRole.CASHIER));
    dishId =
        dishRepository
            .save(new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true))
            .getId();
  }

  @AfterEach
  void tearDown() {
    clean();
    for (String user : new String[] {"cocina1", "reparto1", "caja1"}) {
      staffRepository.findByUsername(user).ifPresent(staffRepository::delete);
    }
  }

  private void clean() {
    ticketRepository.deleteAll();
    paymentRepository.deleteAll();
    orderRepository.deleteAll();
    dishRepository.deleteAll();
    for (String user : new String[] {"cocina1", "reparto1", "caja1"}) {
      staffRepository.findByUsername(user).ifPresent(staffRepository::delete);
    }
  }

  private String token(String username, String password) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(body, "$.token");
  }

  /** Crea un pedido (el pago en línea está desactivado en las pruebas, así que queda PENDING). */
  private long createOrder() throws Exception {
    String json =
        """
        {"dishId": %d, "quantity": 2, "customerName": "Valentina",
         "deliveryTime": "12:00", "deliveryZone": "SHOPPING_CENTER", "deliveryDetail": "Local 12"}
        """
            .formatted(dishId);
    String body =
        mockMvc
            .perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private void changeStatus(long id, String newStatus, String authorization, int expectedStatus)
      throws Exception {
    mockMvc
        .perform(
            patch("/api/staff/orders/{id}/status", id)
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + newStatus + "\"}"))
        .andExpect(status().is(expectedStatus));
  }

  @Test
  void theOrderTravelsFromKitchenToDeliveryAndEachRoleDoesOnlyItsPart() throws Exception {
    long id = createOrder();
    String kitchen = token("cocina1", "clave-1234");
    String delivery = token("reparto1", "clave-1234");

    // Al confirmarse el pedido, la cocina tiene su comanda.
    mockMvc
        .perform(get("/api/staff/kitchen/tickets").header("Authorization", kitchen))
        .andExpect(jsonPath("$.length()").value(1));
    // Entrega todavía no ve nada: el pedido no está en preparación.
    mockMvc
        .perform(get("/api/staff/delivery/orders").header("Authorization", delivery))
        .andExpect(jsonPath("$.length()").value(0));

    // Entrega no puede pasar un pedido a preparación.
    mockMvc
        .perform(
            patch("/api/staff/orders/{id}/status", id)
                .header("Authorization", delivery)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"IN_PREPARATION\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(containsString("DELIVERY")));

    // Cocina sí, y queda registrado quién tomó la comanda.
    changeStatus(id, "IN_PREPARATION", kitchen, 200);
    KitchenTicket ticket = ticketRepository.findAll().get(0);
    assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ACKNOWLEDGED);
    assertThat(ticket.getAcknowledgedBy().getUsername()).isEqualTo("cocina1");
    // La comanda sigue en cocina hasta que la dejen lista.
    mockMvc
        .perform(get("/api/staff/kitchen/tickets").header("Authorization", kitchen))
        .andExpect(jsonPath("$.length()").value(1));

    // Entrega todavía no ve el pedido: cocina no lo ha dejado listo.
    mockMvc
        .perform(get("/api/staff/delivery/orders").header("Authorization", delivery))
        .andExpect(jsonPath("$.length()").value(0));

    // Mientras cocina no lo deje listo, entrega no ve nada.
    mockMvc
        .perform(get("/api/staff/delivery/orders").header("Authorization", delivery))
        .andExpect(jsonPath("$.length()").value(0));
    changeStatus(id, "READY", delivery, 403);
    changeStatus(id, "READY", kitchen, 200);
    mockMvc
        .perform(get("/api/staff/kitchen/tickets").header("Authorization", kitchen))
        .andExpect(jsonPath("$.length()").value(0));

    // Ahora sí lo ve entrega, con a quién y dónde, pero sin precios.
    mockMvc
        .perform(get("/api/staff/delivery/orders").header("Authorization", delivery))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].customerName").value("Valentina"))
        .andExpect(jsonPath("$[0].deliveryDetail").value("Local 12"))
        .andExpect(jsonPath("$[0].total").doesNotExist());

    // Cocina no puede marcarlo como entregado; entrega sí.
    changeStatus(id, "DELIVERED", kitchen, 403);
    changeStatus(id, "DELIVERED", delivery, 200);
    mockMvc
        .perform(get("/api/staff/delivery/orders").header("Authorization", delivery))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void theCashierCancelsAnOrderButCannotCookIt() throws Exception {
    long id = createOrder();
    String cashier = token("caja1", "clave-1234");

    changeStatus(id, "IN_PREPARATION", cashier, 403);
    changeStatus(id, "CANCELLED", cashier, 200);
    // Un pedido cancelado es final: ya nadie lo modifica.
    changeStatus(id, "IN_PREPARATION", token("staff", "test-password"), 400);
  }

  @Test
  void theAdminCanDoAnyValidChange() throws Exception {
    long id = createOrder();
    String admin = token("staff", "test-password");

    changeStatus(id, "IN_PREPARATION", admin, 200);
    changeStatus(id, "READY", admin, 200);
    changeStatus(id, "DELIVERED", admin, 200);
  }

  @Test
  void deliveryCanFilterItsOrdersByZone() throws Exception {
    long id = createOrder();
    changeStatus(id, "IN_PREPARATION", token("staff", "test-password"), 200);
    changeStatus(id, "READY", token("staff", "test-password"), 200);
    String delivery = token("reparto1", "clave-1234");

    mockMvc
        .perform(
            get("/api/staff/delivery/orders")
                .param("zone", "SHOPPING_CENTER")
                .header("Authorization", delivery))
        .andExpect(jsonPath("$.length()").value(1));
    mockMvc
        .perform(
            get("/api/staff/delivery/orders")
                .param("zone", "FURNITURE_STREET")
                .header("Authorization", delivery))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void eachRoleStaysOutOfTheOthersScreens() throws Exception {
    String kitchen = token("cocina1", "clave-1234");
    String delivery = token("reparto1", "clave-1234");
    String cashier = token("caja1", "clave-1234");

    mockMvc
        .perform(get("/api/staff/delivery/orders").header("Authorization", kitchen))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/staff/kitchen/tickets").header("Authorization", delivery))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/staff/cashier/orders").header("Authorization", delivery))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/staff/kitchen/orders").header("Authorization", cashier))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/staff/delivery/orders")).andExpect(status().isUnauthorized());
  }

  @Test
  void theDeliveryRoleGetsItsSectionAtLogin() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"reparto1\", \"password\": \"clave-1234\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("DELIVERY"))
        .andExpect(jsonPath("$.sections.length()").value(1))
        .andExpect(jsonPath("$.sections[0]").value("entrega"));
  }
}
