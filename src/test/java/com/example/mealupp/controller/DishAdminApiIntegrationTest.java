package com.example.mealupp.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Dish;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.DishRepository;
import com.example.mealupp.repository.KitchenTicketRepository;
import com.example.mealupp.repository.OrderRepository;
import com.example.mealupp.repository.PaymentRepository;
import com.example.mealupp.repository.StaffMemberRepository;
import com.jayway.jsonpath.JsonPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * La administradora gestiona el menú y las fotos; el cliente ve el resultado en el menú público.
 */
@SpringBootTest
@ActiveProfiles("test")
class DishAdminApiIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private KitchenTicketRepository ticketRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private DishRepository dishRepository;
  @Autowired private StaffMemberRepository staffRepository;
  @Autowired private PasswordEncoder encoder;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    cleanOrdersAndDishes();
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
  }

  @AfterEach
  void tearDown() {
    cleanOrdersAndDishes();
    staffRepository.findByUsername("cocina1").ifPresent(staffRepository::delete);
  }

  private void cleanOrdersAndDishes() {
    ticketRepository.deleteAll();
    paymentRepository.deleteAll();
    orderRepository.deleteAll();
    dishRepository.deleteAll();
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

  private long createDish(String admin, String name) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/staff/dishes")
                    .header("Authorization", admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\": \""
                            + name
                            + "\", \"description\": \"Con arroz\", \"price\": 12000}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private static byte[] png() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB), "png", out);
    return out.toByteArray();
  }

  // ---------- Crear, editar y disponibilidad ----------

  @Test
  void aNewDishAppearsInTheAdminListAndInThePublicMenu() throws Exception {
    String admin = token("staff", "test-password");

    mockMvc
        .perform(
            post("/api/staff/dishes")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\": \"Pasta Alfredo\", \"description\": \"Con pollo\", \"price\": 11000}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Pasta Alfredo"))
        .andExpect(jsonPath("$.available").value(true))
        .andExpect(jsonPath("$.thumbnailUrl").isEmpty());

    mockMvc
        .perform(get("/api/staff/dishes").header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mockMvc
        .perform(get("/api/menu"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dishes[0].name").value("Pasta Alfredo"));
  }

  @Test
  void aDuplicateNameAndAZeroPriceAreRejected() throws Exception {
    String admin = token("staff", "test-password");
    createDish(admin, "Pasta Alfredo");

    mockMvc
        .perform(
            post("/api/staff/dishes")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"pasta alfredo\", \"price\": 9000}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Ya existe un plato con ese nombre"));
    mockMvc
        .perform(
            post("/api/staff/dishes")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Gratis\", \"price\": 0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("El precio debe ser mayor que 0"));
    mockMvc
        .perform(
            post("/api/staff/dishes")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\": \"sin nombre\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void editingADishChangesWhatTheCustomerSees() throws Exception {
    String admin = token("staff", "test-password");
    long id = createDish(admin, "Pollo a la plancha");

    mockMvc
        .perform(
            put("/api/staff/dishes/{id}", id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\": \"Pollo asado\", \"description\": \"Con papas\", \"price\": 13500}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Pollo asado"))
        .andExpect(jsonPath("$.price").value(13500));
    mockMvc.perform(get("/api/menu")).andExpect(jsonPath("$.dishes[0].name").value("Pollo asado"));
  }

  @Test
  void anUnavailableDishDisappearsFromTheMenuButNotFromTheAdminList() throws Exception {
    String admin = token("staff", "test-password");
    long id = createDish(admin, "Pasta");

    mockMvc
        .perform(
            patch("/api/staff/dishes/{id}/availability", id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"available\": false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(false));

    mockMvc
        .perform(get("/api/menu"))
        .andExpect(jsonPath("$.dishes.length()").value(0))
        .andExpect(jsonPath("$.message").value("No hay menú disponible hoy"));
    mockMvc
        .perform(get("/api/staff/dishes").header("Authorization", admin))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void editingAnUnknownDishReturns404() throws Exception {
    String admin = token("staff", "test-password");

    mockMvc
        .perform(
            put("/api/staff/dishes/{id}", 99999)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"X\", \"price\": 1000}"))
        .andExpect(status().isNotFound());
  }

  // ---------- Fotos ----------

  @Test
  void thePhotoIsStoredServedAndShownInTheMenu() throws Exception {
    String admin = token("staff", "test-password");
    long id = createDish(admin, "Pollo a la plancha");
    MockMultipartFile file = new MockMultipartFile("file", "plato.png", "image/png", png());

    String body =
        mockMvc
            .perform(
                multipart("/api/staff/dishes/{id}/photo", id)
                    .file(file)
                    .header("Authorization", admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.thumbnailUrl").value(startsWith("/media/dishes/" + id + "/")))
            .andExpect(jsonPath("$.thumbnailUrl").value(endsWith("_thumb.jpg")))
            .andExpect(jsonPath("$.imageUrl").value(endsWith("_detail.jpg")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String thumbnailUrl = JsonPath.read(body, "$.thumbnailUrl");

    mockMvc
        .perform(get(thumbnailUrl))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/jpeg"));
    mockMvc
        .perform(get("/api/menu"))
        .andExpect(jsonPath("$.dishes[0].thumbnailUrl").value(thumbnailUrl));
  }

  @Test
  void aFileThatIsNotAnImageIsRejected() throws Exception {
    String admin = token("staff", "test-password");
    long id = createDish(admin, "Pollo a la plancha");
    MockMultipartFile file =
        new MockMultipartFile("file", "nota.txt", "text/plain", "hola".getBytes());

    mockMvc
        .perform(
            multipart("/api/staff/dishes/{id}/photo", id).file(file).header("Authorization", admin))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("El archivo no es una imagen JPG o PNG válida"));
  }

  @Test
  void deletingThePhotoLeavesTheDishWithoutImage() throws Exception {
    String admin = token("staff", "test-password");
    long id = createDish(admin, "Pollo a la plancha");
    mockMvc
        .perform(
            multipart("/api/staff/dishes/{id}/photo", id)
                .file(new MockMultipartFile("file", "plato.png", "image/png", png()))
                .header("Authorization", admin))
        .andExpect(status().isOk());

    mockMvc
        .perform(delete("/api/staff/dishes/{id}/photo", id).header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.thumbnailUrl").isEmpty())
        .andExpect(jsonPath("$.imageUrl").isEmpty());
  }

  // ---------- Borrar ----------

  @Test
  void aDishWithoutOrdersCanBeDeleted() throws Exception {
    String admin = token("staff", "test-password");
    long id = createDish(admin, "Pasta");

    mockMvc
        .perform(delete("/api/staff/dishes/{id}", id).header("Authorization", admin))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/staff/dishes").header("Authorization", admin))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void aDishThatAlreadyHasOrdersCannotBeDeleted() throws Exception {
    String admin = token("staff", "test-password");
    long id = createDish(admin, "Pasta");
    Dish dish = dishRepository.findById(id).orElseThrow();
    orderRepository.save(
        new Order(
            dish,
            1,
            "Valentina",
            LocalDate.of(2026, 11, 4),
            LocalTime.of(12, 0),
            DeliveryZone.SHOPPING_CENTER,
            "Local 12"));

    mockMvc
        .perform(delete("/api/staff/dishes/{id}", id).header("Authorization", admin))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("agotado")));
  }

  // ---------- Permisos ----------

  @Test
  void onlyTheAdminCanManageTheMenu() throws Exception {
    String kitchen = token("cocina1", "clave-1234");

    mockMvc
        .perform(get("/api/staff/dishes").header("Authorization", kitchen))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/staff/dishes")
                .header("Authorization", kitchen)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"X\", \"price\": 1000}"))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/staff/dishes")).andExpect(status().isUnauthorized());
  }

  @Test
  void theMenuStaysPublicAndKeepsTheBigDecimalPrice() throws Exception {
    dishRepository.save(new Dish("Pasta", "Con salsa", new BigDecimal("11000"), true));

    mockMvc
        .perform(get("/api/menu"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dishes[0].price").value(11000));
  }
}
