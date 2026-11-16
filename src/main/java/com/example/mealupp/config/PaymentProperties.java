package com.example.mealupp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de los pagos en línea.
 *
 * @param required si es verdadero, el pedido queda pendiente de pago hasta que la pasarela lo
 *     confirme; si es falso, queda confirmado de inmediato
 * @param provider proveedor de la pasarela (hoy solo existe "sandbox", una pasarela simulada)
 * @param webhookSecret clave con la que la pasarela firma sus avisos
 * @param expirationMinutes minutos que se espera el pago antes de vencer el pedido
 * @param currency moneda de los cobros
 * @param sandboxCheckoutUrl página del frontend que simula el pago en la pasarela de pruebas
 */
@ConfigurationProperties(prefix = "mealup.payments")
public record PaymentProperties(
    boolean required,
    String provider,
    String webhookSecret,
    int expirationMinutes,
    String currency,
    String sandboxCheckoutUrl) {}
