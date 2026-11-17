package com.example.mealupp.config;

import com.example.mealupp.integration.payment.PaymentGateway;
import com.example.mealupp.integration.payment.SandboxPaymentGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pasarela de pagos. Hoy solo existe la simulada (provider=sandbox). Cuando se elija un proveedor
 * real, se agrega aquí un adaptador nuevo para su valor de provider.
 */
@Configuration
public class PaymentConfig {

  @Bean
  @ConditionalOnProperty(
      name = "mealup.payments.provider",
      havingValue = "sandbox",
      matchIfMissing = true)
  public PaymentGateway sandboxPaymentGateway(PaymentProperties properties) {
    return new SandboxPaymentGateway(properties.sandboxCheckoutUrl());
  }
}
