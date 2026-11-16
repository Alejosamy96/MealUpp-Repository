package com.example.mealupp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.KitchenTicketRepository;
import com.example.mealupp.repository.PaymentRepository;
import com.example.mealupp.repository.StaffMemberRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** La administradora registra al personal, lo activa o desactiva y nadie más puede hacerlo. */
@SpringBootTest
@ActiveProfiles("test")
class StaffManagementApiIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private KitchenTicketRepository ticketRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private StaffMemberRepository staffRepository;
  @Autowired private PasswordEncoder encoder;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    ticketRepository.deleteAll();
    paymentRepository.deleteAll();
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    removeTestPeople();
    if (staffRepository.findByUsername("staff").isEmpty()) {
      staffRepository.save(
          new StaffMember(
              "staff", "Administradora", encoder.encode("test-password"), StaffRole.ADMIN));
    }
  }

  @AfterEach
  void tearDown() {
    removeTestPeople();
  }

  private void removeTestPeople() {
    for (String user : new String[] {"nuevo.cocina", "segunda.admin"}) {
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

  private long createPerson(String admin, String username, String role) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/staff/members")
                    .header("Authorization", admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\": \""
                            + username
                            + "\", \"fullName\": \"Persona Nueva\","
                            + " \"password\": \"clave-1234\", \"role\": \""
                            + role
                            + "\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  @Test
  void aNewPersonCanLogInWithTheRoleTheAdminGaveThem() throws Exception {
    String admin = token("staff", "test-password");

    mockMvc
        .perform(
            post("/api/staff/members")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\": \"Nuevo.Cocina\", \"fullName\": \"Persona Nueva\","
                        + " \"password\": \"clave-1234\", \"role\": \"KITCHEN\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("nuevo.cocina"))
        .andExpect(jsonPath("$.role").value("KITCHEN"))
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andExpect(jsonPath("$.password").doesNotExist());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"nuevo.cocina\", \"password\": \"clave-1234\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("KITCHEN"));
  }

  @Test
  void invalidDataIsRejected() throws Exception {
    String admin = token("staff", "test-password");
    createPerson(admin, "nuevo.cocina", "KITCHEN");

    String[] bodies = {
      "{\"username\": \"nuevo.cocina\", \"fullName\": \"X\", \"password\": \"clave-1234\", \"role\": \"KITCHEN\"}",
      "{\"username\": \"ab\", \"fullName\": \"X\", \"password\": \"clave-1234\", \"role\": \"KITCHEN\"}",
      "{\"username\": \"con espacio\", \"fullName\": \"X\", \"password\": \"clave-1234\", \"role\": \"KITCHEN\"}",
      "{\"username\": \"otra.persona\", \"fullName\": \"X\", \"password\": \"corta\", \"role\": \"KITCHEN\"}",
      "{\"username\": \"otra.persona\", \"fullName\": \"X\", \"password\": \"clave-1234\", \"role\": \"JEFE\"}",
      "{\"username\": \"otra.persona\", \"password\": \"clave-1234\", \"role\": \"KITCHEN\"}"
    };
    for (String body : bodies) {
      mockMvc
          .perform(
              post("/api/staff/members")
                  .header("Authorization", admin)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest());
    }
  }

  @Test
  void aDeactivatedPersonCannotLogInAndTheirTokenStopsWorking() throws Exception {
    String admin = token("staff", "test-password");
    long id = createPerson(admin, "nuevo.cocina", "KITCHEN");
    String personToken = token("nuevo.cocina", "clave-1234");
    mockMvc
        .perform(get("/api/staff/kitchen/orders").header("Authorization", personToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/staff/members/{id}/active", id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\": false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    mockMvc
        .perform(get("/api/staff/kitchen/orders").header("Authorization", personToken))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"nuevo.cocina\", \"password\": \"clave-1234\"}"))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            patch("/api/staff/members/{id}/active", id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\": true}"))
        .andExpect(jsonPath("$.active").value(true));
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"nuevo.cocina\", \"password\": \"clave-1234\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void theLastAdminCannotBeDeactivatedButOneOfTwoCan() throws Exception {
    String admin = token("staff", "test-password");
    long staffId = staffRepository.findByUsername("staff").orElseThrow().getId();

    mockMvc
        .perform(
            patch("/api/staff/members/{id}/active", staffId)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\": false}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Debe quedar al menos una persona con rol ADMIN"));

    long secondId = createPerson(admin, "segunda.admin", "ADMIN");
    mockMvc
        .perform(
            patch("/api/staff/members/{id}/active", secondId)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\": false}"))
        .andExpect(status().isOk());
  }

  @Test
  void onlyTheAdminCanRegisterOrDeactivatePeople() throws Exception {
    String admin = token("staff", "test-password");
    long id = createPerson(admin, "nuevo.cocina", "KITCHEN");
    String kitchen = token("nuevo.cocina", "clave-1234");

    mockMvc
        .perform(
            post("/api/staff/members")
                .header("Authorization", kitchen)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\": \"intruso\", \"fullName\": \"X\", \"password\": \"clave-1234\", \"role\": \"ADMIN\"}"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            patch("/api/staff/members/{id}/active", id)
                .header("Authorization", kitchen)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\": false}"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/staff/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\": \"intruso\", \"fullName\": \"X\", \"password\": \"clave-1234\", \"role\": \"ADMIN\"}"))
        .andExpect(status().isUnauthorized());
  }
}
