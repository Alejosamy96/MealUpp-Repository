package com.example.mealupp.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentExpirationJobTest {

  @Mock private PaymentService paymentService;

  @Test
  void theJobAsksTheServiceToExpireTheStaleOrders() {
    when(paymentService.expireStaleOrders()).thenReturn(2);

    new PaymentExpirationJob(paymentService).expireStaleOrders();

    verify(paymentService).expireStaleOrders();
  }

  @Test
  void theJobAlsoWorksWhenThereIsNothingToExpire() {
    when(paymentService.expireStaleOrders()).thenReturn(0);

    new PaymentExpirationJob(paymentService).expireStaleOrders();

    verify(paymentService).expireStaleOrders();
  }
}
