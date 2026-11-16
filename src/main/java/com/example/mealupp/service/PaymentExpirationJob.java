package com.example.mealupp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tarea periódica que vence los pedidos que no se pagaron a tiempo. */
@Component
public class PaymentExpirationJob {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentExpirationJob.class);

  private final PaymentService paymentService;

  public PaymentExpirationJob(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @Scheduled(fixedDelayString = "${mealup.payments.expiration-check-ms}")
  public void expireStaleOrders() {
    int expired = paymentService.expireStaleOrders();
    if (expired > 0) {
      LOG.info("Se vencieron {} pedidos sin pagar", expired);
    }
  }
}
