package com.example.mealupp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** El frontend lee las reglas del negocio en lugar de escribirlas a mano. */
@SpringBootTest
@ActiveProfiles("test")
class PublicConfigApiIntegrationTest {

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
  }

  @Test
  void theRulesArePublic() throws Exception {
    mockMvc
        .perform(get("/api/config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timeZone").value("America/Bogota"))
        .andExpect(jsonPath("$.cutoffTime").value("10:30:00"))
        .andExpect(jsonPath("$.deliveryStart").value("11:30:00"))
        .andExpect(jsonPath("$.deliveryEnd").value("14:00:00"))
        .andExpect(jsonPath("$.minQuantity").value(1))
        .andExpect(jsonPath("$.maxQuantity").value(10))
        .andExpect(jsonPath("$.maxOrdersPerDay").value(100))
        .andExpect(jsonPath("$.currency").value("COP"))
        .andExpect(jsonPath("$.paymentsRequired").value(false));
  }

  @Test
  void theZonesComeWithTheirCodeAndLabel() throws Exception {
    mockMvc
        .perform(get("/api/config"))
        .andExpect(jsonPath("$.zones.length()").value(2))
        .andExpect(jsonPath("$.zones[0].code").value("SHOPPING_CENTER"))
        .andExpect(jsonPath("$.zones[0].label").value("Centro comercial"))
        .andExpect(jsonPath("$.zones[1].code").value("FURNITURE_STREET"))
        .andExpect(jsonPath("$.zones[1].label").value("Calle de los Muebles"));
  }
}
