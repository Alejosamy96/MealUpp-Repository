package com.example.mealupp.controller;

import com.example.mealupp.config.OrderRulesProperties;
import com.example.mealupp.config.PaymentProperties;
import com.example.mealupp.dto.PublicConfigResponse;
import com.example.mealupp.model.DeliveryZone;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reglas públicas del negocio, para que el frontend no las escriba a mano. */
@RestController
@RequestMapping("/api/config")
public class PublicConfigController {

  private final OrderRulesProperties rules;
  private final PaymentProperties payments;
  private final String timeZone;

  public PublicConfigController(
      OrderRulesProperties rules,
      PaymentProperties payments,
      @Value("${mealup.time-zone}") String timeZone) {
    this.rules = rules;
    this.payments = payments;
    this.timeZone = timeZone;
  }

  @GetMapping
  public PublicConfigResponse get() {
    List<PublicConfigResponse.Zone> zones =
        Arrays.stream(DeliveryZone.values())
            .map(zone -> new PublicConfigResponse.Zone(zone.name(), zone.label()))
            .toList();
    return new PublicConfigResponse(
        timeZone,
        rules.cutoffTime(),
        rules.deliveryStart(),
        rules.deliveryEnd(),
        rules.minQuantity(),
        rules.maxQuantity(),
        rules.maxPerDay(),
        payments.required(),
        payments.currency(),
        payments.expirationMinutes(),
        zones);
  }
}
