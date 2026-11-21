package com.example.mealupp.config;

import com.example.mealupp.integration.payment.MercadoPagoClient;
import com.example.mealupp.integration.payment.MercadoPagoPaymentGateway;
import com.example.mealupp.integration.payment.PaymentGateway;
import com.example.mealupp.integration.payment.SandboxPaymentGateway;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Pasarela de pagos. provider=sandbox usa la pasarela simulada (desarrollo y demostraciones);
 * provider=mercadopago usa Mercado Pago (Checkout Pro). Para otro proveedor se agrega aquí su
 * adaptador.
 */
@Configuration
public class PaymentConfig {

  @Bean
  @Primary
  @ConditionalOnProperty(
      name = "mealup.payments.provider",
      havingValue = "sandbox",
      matchIfMissing = true)
  public PaymentGateway sandboxPaymentGateway(PaymentProperties properties) {
    return new SandboxPaymentGateway(properties.sandboxCheckoutUrl());
  }

  @Bean
  @ConditionalOnProperty(name = "mealup.payments.provider", havingValue = "mercadopago")
  public MercadoPagoClient mercadoPagoClient(MercadoPagoProperties properties) {
    if (properties.accessToken() == null || properties.accessToken().isBlank()) {
      throw new IllegalStateException(
          "Falta MERCADOPAGO_ACCESS_TOKEN: es obligatorio con PAYMENT_PROVIDER=mercadopago");
    }
    return new MercadoPagoClient(properties.apiUrl(), properties.accessToken());
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "mealup.payments.provider", havingValue = "mercadopago")
  public PaymentGateway mercadoPagoPaymentGateway(
      MercadoPagoClient client,
      MercadoPagoProperties properties,
      PaymentProperties payments,
      Clock clock) {
    return new MercadoPagoPaymentGateway(
        client,
        properties.notificationUrl(),
        properties.frontendUrl(),
        payments.expirationMinutes(),
        clock);
  }
}
