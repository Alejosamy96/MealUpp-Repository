package com.example.mealupp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.mealupp.model.StaffMember;
import com.example.mealupp.model.StaffRole;
import com.example.mealupp.repository.StaffMemberRepository;
import com.jayway.jsonpath.JsonPath;
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

/** HU 6 de punta a punta: la administradora asigna roles y nadie más puede hacerlo. */
@SpringBootTest
@ActiveProfiles("test")
class StaffMemberApiIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private StaffMemberRepository staffRepository;
  @Autowired private PasswordEncoder encoder;

  private MockMvc mockMvc;
  private Long carlosId;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    staffRepository.findAll().stream()
        .filter(m -> m.getRole() != StaffRole.ADMIN)
        .forEach(staffRepository::delete);
    // HU 6 no crea usuarios: la persona de prueba se registra directo en la base de datos.
    carlosId =
        staffRepository
            .save(
                new StaffMember(
                    "carlos", "Carlos Pérez", encoder.encode("clave-1234"), StaffRole.KITCHEN))
            .getId();
  }

  private String token(String username, String password) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\": \"%s\", \"password\": \"%s\"}"
                            .formatted(username, password)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(body, "$.token");
  }

  @Test
  void hu6_adminAssignsARoleAndItIsSaved() throws Exception {
    mockMvc
        .perform(
            patch("/api/staff/members/{id}/role", carlosId)
                .header("Authorization", token("staff", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"CASHIER\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("CASHIER"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"carlos\", \"password\": \"clave-1234\"}"))
        .andExpect(jsonPath("$.role").value("CASHIER"));
  }

  @Test
  void hu6_kitchenCannotChangeRoles() throws Exception {
    mockMvc
        .perform(
            patch("/api/staff/members/{id}/role", carlosId)
                .header("Authorization", token("carlos", "clave-1234"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"ADMIN\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void hu6_kitchenCannotOpenAnotherRolesPageByDirectAddress() throws Exception {
    mockMvc
        .perform(get("/api/staff/members").header("Authorization", token("carlos", "clave-1234")))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/staff/orders").header("Authorization", token("carlos", "clave-1234")))
        .andExpect(status().isForbidden());
  }
}
