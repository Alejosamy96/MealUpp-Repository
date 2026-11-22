package com.example.mealupp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de Mercado Pago (Checkout Pro). Solo se usa con
 * mealup.payments.provider=mercadopago.
 *
 * @param accessToken credencial privada de la aplicación (empieza por TEST- en pruebas o APP_USR-
 *     en producción)
 * @param webhookSecret clave secreta de los webhooks, la que muestra el panel de Mercado Pago
 * @param apiUrl dirección de la API de Mercado Pago
 * @param notificationUrl dirección pública (HTTPS) del webhook de este backend
 * @param frontendUrl dirección del frontend, a donde vuelve el cliente después de pagar
 */
@ConfigurationProperties(prefix = "mealup.payments.mercadopago")
public record MercadoPagoProperties(
    String accessToken,
    String webhookSecret,
    String apiUrl,
    String notificationUrl,
    String frontendUrl) {}
