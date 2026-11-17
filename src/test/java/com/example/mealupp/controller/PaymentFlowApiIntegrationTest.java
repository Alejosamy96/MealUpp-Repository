package com.example.mealupp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Dish;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.model.PaymentStatus;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.DishRepository;
import com.example.mealupp.repository.KitchenTicketRepository;
import com.example.mealupp.repository.OrderRepository;
import com.example.mealupp.repository.PaymentRepository;
import com.example.mealupp.repository.StaffMemberRepository;
import com.example.mealupp.service.PaymentService;
import com.example.mealupp.util.HmacSigner;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

/** Pago en línea de punta a punta, con la pasarela simulada. Aquí el pago es obligatorio. */
@SpringBootTest(properties = "mealup.payments.required=true")
@ActiveProfiles("test")
class PaymentFlowApiIntegrationTest {

  private static final String WEBHOOK_SECRET = "secreto-del-webhook-de-pruebas";

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
  @Autowired private PaymentService paymentService;

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
    if (staffRepository.findByUsername("cocina1").isEmpty()) {
      staffRepository.save(
          new StaffMember("cocina1", "Carlos", encoder.encode("clave-1234"), StaffRole.KITCHEN));
    }
    dishId =
        dishRepository
            .save(new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true))
            .getId();
  }

  @AfterEach
  void tearDown() {
    clean();
    staffRepository.findByUsername("cocina1").ifPresent(staffRepository::delete);
  }

  private void clean() {
    ticketRepository.deleteAll();
    paymentRepository.deleteAll();
    orderRepository.deleteAll();
    dishRepository.deleteAll();
  }

  private String orderJson() {
    return """
        {"dishId": %d, "quantity": 2, "customerName": "Valentina",
         "deliveryTime": "12:00", "deliveryZone": "SHOPPING_CENTER", "deliveryDetail": "Local 12"}
        """
        .formatted(dishId);
  }

  /** Crea un pedido y devuelve su respuesta JSON. */
  private String createOrder() throws Exception {
    return mockMvc
        .perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson()))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private static String referenceOf(String createdOrderJson) {
    String url = JsonPath.read(createdOrderJson, "$.paymentUrl");
    return url.substring(url.indexOf("reference=") + "reference=".length());
  }

  private static long idOf(String json) {
    return ((Number) JsonPath.read(json, "$.id")).longValue();
  }

  private String kitchenToken() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\": \"cocina1\", \"password\": \"clave-1234\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(body, "$.token");
  }

  // ---------- Crear el pedido ----------

  @Test
  void theOrderWaitsForPaymentAndGivesTheCustomerALinkAndACode() throws Exception {
    String created = createOrder();

    assertThat((String) JsonPath.read(created, "$.status")).isEqualTo("PENDING_PAYMENT");
    assertThat((String) JsonPath.read(created, "$.paymentUrl"))
        .startsWith("http://localhost:5173/pago-simulado?reference=SBX-");
    assertThat((String) JsonPath.read(created, "$.accessCode")).hasSize(10);
    assertThat(((Number) JsonPath.read(created, "$.total")).intValue()).isEqualTo(24000);
    assertThat(ticketRepository.count()).isZero();
    assertThat(paymentRepository.findAll()).hasSize(1);
  }

  @Test
  void theCustomerSeesTheOrderWithTheCodeAndOnlyWithTheCode() throws Exception {
    String created = createOrder();
    long id = idOf(created);
    String code = JsonPath.read(created, "$.accessCode");

    mockMvc
        .perform(get("/api/orders/{id}", id).param("code", code))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
        .andExpect(jsonPath("$.paymentPending").value(true))
        .andExpect(jsonPath("$.dishName").value("Pollo a la plancha"));
    mockMvc
        .perform(get("/api/orders/{id}", id).param("code", "CODIGO-FALSO"))
        .andExpect(status().isNotFound());
    mockMvc.perform(get("/api/orders/{id}", id)).andExpect(status().isBadRequest());
  }

  // ---------- Pago aprobado ----------

  @Test
  void approvingThePaymentConfirmsTheOrderAndSendsTheTicketToTheKitchen() throws Exception {
    String created = createOrder();
    long id = idOf(created);
    String code = JsonPath.read(created, "$.accessCode");

    mockMvc
        .perform(post("/api/payments/sandbox/{ref}/approve", referenceOf(created)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.received").value(true));

    mockMvc
        .perform(get("/api/orders/{id}", id).param("code", code))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.paymentPending").value(false));
    assertThat(ticketRepository.count()).isEqualTo(1);
    mockMvc
        .perform(get("/api/staff/kitchen/tickets").header("Authorization", kitchenToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].dishName").value("Pollo a la plancha"))
        .andExpect(jsonPath("$[0].quantity").value(2))
        .andExpect(jsonPath("$[0].customerName").doesNotExist());
  }

  @Test
  void approvingTwiceDoesNotCreateASecondTicket() throws Exception {
    String created = createOrder();
    String reference = referenceOf(created);

    mockMvc
        .perform(post("/api/payments/sandbox/{ref}/approve", reference))
        .andExpect(status().isOk());
    mockMvc
        .perform(post("/api/payments/sandbox/{ref}/approve", reference))
        .andExpect(status().isOk());

    assertThat(ticketRepository.count()).isEqualTo(1);
  }

  @Test
  void aSignedWebhookApprovesThePayment() throws Exception {
    String created = createOrder();
    long id = idOf(created);
    String code = JsonPath.read(created, "$.accessCode");
    String body =
        "{\"reference\":\"" + referenceOf(created) + "\",\"status\":\"APPROVED\",\"amount\":24000}";

    mockMvc
        .perform(
            post("/api/payments/webhook")
                .header("X-Signature", HmacSigner.hmacHex(WEBHOOK_SECRET, body))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.received").value(true));

    mockMvc
        .perform(get("/api/orders/{id}", id).param("code", code))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void aWebhookWithABadOrMissingSignatureIsRejectedAndChangesNothing() throws Exception {
    String created = createOrder();
    String body = "{\"reference\":\"" + referenceOf(created) + "\",\"status\":\"APPROVED\"}";

    mockMvc
        .perform(
            post("/api/payments/webhook")
                .header("X-Signature", "firma-falsa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/payments/webhook").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized());

    assertThat(ticketRepository.count()).isZero();
    assertThat(orderRepository.findAll().get(0).getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
  }

  @Test
  void aWebhookWithADifferentAmountIsRejected() throws Exception {
    String created = createOrder();
    String body =
        "{\"reference\":\"" + referenceOf(created) + "\",\"status\":\"APPROVED\",\"amount\":1000}";

    mockMvc
        .perform(
            post("/api/payments/webhook")
                .header("X-Signature", HmacSigner.hmacHex(WEBHOOK_SECRET, body))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("monto")));
    assertThat(ticketRepository.count()).isZero();
  }

  // ---------- Pago rechazado y reintento ----------

  @Test
  void aDeclinedPaymentKeepsTheOrderPendingAndTheCustomerCanTryAgain() throws Exception {
    String created = createOrder();
    long id = idOf(created);
    String code = JsonPath.read(created, "$.accessCode");
    String firstUrl = JsonPath.read(created, "$.paymentUrl");

    mockMvc
        .perform(post("/api/payments/sandbox/{ref}/decline", referenceOf(created)))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/orders/{id}", id).param("code", code))
        .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

    String retry =
        mockMvc
            .perform(post("/api/orders/{id}/pay", id).param("code", code))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentUrl").value(containsString("reference=SBX-")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat((String) JsonPath.read(retry, "$.paymentUrl")).isNotEqualTo(firstUrl);
    assertThat(paymentRepository.findAll()).hasSize(2);
    assertThat(paymentRepository.findAll())
        .extracting(payment -> payment.getStatus())
        .containsExactlyInAnyOrder(PaymentStatus.DECLINED, PaymentStatus.PENDING);
  }

  @Test
  void retryNeedsTheRightCodeAndAnOrderStillPendingPayment() throws Exception {
    String created = createOrder();
    long id = idOf(created);
    String code = JsonPath.read(created, "$.accessCode");

    mockMvc
        .perform(post("/api/orders/{id}/pay", id).param("code", "CODIGO-FALSO"))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(post("/api/payments/sandbox/{ref}/approve", referenceOf(created)))
        .andExpect(status().isOk());
    mockMvc
        .perform(post("/api/orders/{id}/pay", id).param("code", code))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("El pedido ya no está pendiente de pago"));
  }

  @Test
  void anUnknownReferenceIsNotFound() throws Exception {
    mockMvc
        .perform(post("/api/payments/sandbox/{ref}/approve", "SBX-no-existe"))
        .andExpect(status().isNotFound());
  }

  // ---------- Vencimiento ----------

  @Test
  void anOrderUnpaidForMoreThan15MinutesExpires() throws Exception {
    Dish dish = dishRepository.findById(dishId).orElseThrow();
    Order old =
        new Order(
            dish,
            1,
            "Marta",
            LocalDate.of(2026, 11, 4),
            LocalTime.of(12, 0),
            DeliveryZone.SHOPPING_CENTER,
            "Local 3");
    old.setStatus(OrderStatus.PENDING_PAYMENT);
    old.setCreatedAt(LocalDateTime.of(2026, 11, 4, 8, 40));
    Order recent =
        new Order(
            dish,
            1,
            "Luis",
            LocalDate.of(2026, 11, 4),
            LocalTime.of(12, 0),
            DeliveryZone.SHOPPING_CENTER,
            "Local 4");
    recent.setStatus(OrderStatus.PENDING_PAYMENT);
    recent.setCreatedAt(LocalDateTime.of(2026, 11, 4, 8, 50));
    Long oldId = orderRepository.save(old).getId();
    Long recentId = orderRepository.save(recent).getId();

    int expired = paymentService.expireStaleOrders();

    assertThat(expired).isEqualTo(1);
    assertThat(orderRepository.findById(oldId).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.EXPIRED);
    assertThat(orderRepository.findById(recentId).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.PENDING_PAYMENT);
  }

  @Test
  void theConfigurationTellsTheFrontendThatPaymentIsRequired() throws Exception {
    mockMvc
        .perform(get("/api/config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paymentsRequired").value(true))
        .andExpect(jsonPath("$.paymentExpirationMinutes").value(15));
  }
}
