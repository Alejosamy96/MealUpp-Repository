package com.example.mealupp.dto;

/** Respuesta a la pasarela: confirma que el aviso se recibió. */
public record WebhookAck(boolean received) {}
