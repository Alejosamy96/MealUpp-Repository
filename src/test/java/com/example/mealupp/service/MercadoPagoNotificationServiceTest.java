package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.mealupp.config.MercadoPagoProperties;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.InvalidSignatureException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.integration.payment.MercadoPagoClient;
import com.example.mealupp.model.PaymentStatus;
import com.example.mealupp.util.HmacSigner;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MercadoPagoNotificationServiceTest {

  private static final String SECRET = "clave-del-webhook";
  private static final String TS = "1742505638683";

  @Mock private MercadoPagoClient client;
  @Mock private PaymentService paymentService;

  private MercadoPagoNotificationService service;
  private String signature;

  @BeforeEach
  void setUp() {
    MercadoPagoProperties properties =
        new MercadoPagoProperties("TEST-token", SECRET, "https://api.test", "", "http://localhost");
    service = new MercadoPagoNotificationService(client, properties, paymentService);
    signature =
        "ts=" + TS + ",v1=" + HmacSigner.hmacHex(SECRET, "id:777;request-id:req-1;ts:" + TS + ";");
  }

  private void handle() {
    service.handle("777", "payment", signature, "req-1");
  }

  @Test
  void rejectsAnInvalidSignatureBeforeAnything() {
    assertThatThrownBy(() -> service.handle("777", "payment", "ts=1,v1=00", "req-1"))
        .isInstanceOf(InvalidSignatureException.class);

    verifyNoInteractions(client, paymentService);
  }

  @Test
  void approvesTheOrderWhenMercadoPagoSaysApproved() {
    when(client.findPayment("777"))
        .thenReturn(
            Optional.of(
                Map.of(
                    "status",
                    "approved",
                    "external_reference",
                    "MU-1-1",
                    "transaction_amount",
                    new BigDecimal("12000.0"))));

    handle();

    verify(paymentService).applyResult(eq("MU-1-1"), eq(PaymentStatus.APPROVED), any());
  }

  @Test
  void declinesWhenRejectedOrCancelled() {
    when(client.findPayment("777"))
        .thenReturn(Optional.of(Map.of("status", "rejected", "external_reference", "MU-1-1")));

    handle();

    verify(paymentService).applyResult("MU-1-1", PaymentStatus.DECLINED, null);
  }

  @Test
  void ignoresPendingAndInProcessPayments() {
    when(client.findPayment("777"))
        .thenReturn(Optional.of(Map.of("status", "in_process", "external_reference", "MU-1-1")));

    handle();

    verify(paymentService, never()).applyResult(any(), any(), any());
  }

  @Test
  void ignoresOtherTypesAndMissingIds() {
    String other =
        "ts=" + TS + ",v1=" + HmacSigner.hmacHex(SECRET, "id:777;request-id:req-1;ts:" + TS + ";");

    service.handle("777", "merchant_order", other, "req-1");
    service.handle("777", null, other, "req-1");

    verifyNoInteractions(client, paymentService);
  }

  @Test
  void ignoresAPaymentMercadoPagoDoesNotKnow() {
    when(client.findPayment("777")).thenReturn(Optional.empty());

    handle();

    verifyNoInteractions(paymentService);
  }

  @Test
  void ignoresAPaymentWithoutOurReference() {
    when(client.findPayment("777")).thenReturn(Optional.of(Map.of("status", "approved")));

    handle();

    verifyNoInteractions(paymentService);
  }

  @Test
  void ignoresAPaymentThatIsNotFromAnyOrder() {
    when(client.findPayment("777"))
        .thenReturn(Optional.of(Map.of("status", "approved", "external_reference", "OTRO-9")));
    doThrow(new NotFoundException("El pago no existe"))
        .when(paymentService)
        .applyResult(eq("OTRO-9"), eq(PaymentStatus.APPROVED), any());

    handle();

    verify(paymentService).applyResult(eq("OTRO-9"), eq(PaymentStatus.APPROVED), any());
  }

  @Test
  void rejectsAnAmountThatIsNotANumber() {
    when(client.findPayment("777"))
        .thenReturn(
            Optional.of(
                Map.of(
                    "status",
                    "approved",
                    "external_reference",
                    "MU-1-1",
                    "transaction_amount",
                    "mucho")));

    assertThatThrownBy(this::handle).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void mapsOnlyFinalStatuses() {
    assertThat(MercadoPagoNotificationService.toStatus("approved"))
        .isEqualTo(PaymentStatus.APPROVED);
    assertThat(MercadoPagoNotificationService.toStatus("cancelled"))
        .isEqualTo(PaymentStatus.DECLINED);
    assertThat(MercadoPagoNotificationService.toStatus("refunded")).isNull();
    assertThat(MercadoPagoNotificationService.toStatus(null)).isNull();
  }
}
