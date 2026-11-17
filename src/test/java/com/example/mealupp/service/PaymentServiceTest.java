package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mealupp.config.PaymentProperties;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.InvalidSignatureException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.integration.payment.PaymentGateway;
import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.model.Dish;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.model.Payment;
import com.example.mealupp.model.PaymentStatus;
import com.example.mealupp.repository.OrderRepository;
import com.example.mealupp.repository.PaymentRepository;
import com.example.mealupp.util.HmacSigner;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  private static final ZoneId ZONE = ZoneId.of("America/Bogota");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 9, 0);
  private static final String SECRET = "secreto-de-pruebas";
  private static final PaymentProperties PROPERTIES =
      new PaymentProperties(true, "sandbox", SECRET, 15, "COP", "http://localhost/pago");

  @Mock private PaymentRepository paymentRepository;
  @Mock private OrderRepository orderRepository;
  @Mock private PaymentGateway gateway;
  @Mock private OrderConfirmationService confirmation;

  private PaymentService service;
  private Order order;
  private Payment payment;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
    service =
        new PaymentService(
            paymentRepository, orderRepository, gateway, PROPERTIES, confirmation, clock);
    Dish dish = new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true);
    order =
        new Order(
            dish,
            2,
            "Valentina",
            LocalDate.of(2026, 11, 4),
            LocalTime.of(12, 0),
            DeliveryZone.SHOPPING_CENTER,
            "Local 12");
    order.setStatus(OrderStatus.PENDING_PAYMENT);
    order.setCreatedAt(NOW);
    ReflectionTestUtils.setField(order, "id", 7L);
    payment = new Payment(order, "sandbox", new BigDecimal("24000"), "COP", NOW);
    payment.setProviderReference("SBX-1");
  }

  private static String sign(String body) {
    return HmacSigner.hmacHex(SECRET, body);
  }

  // ---------- Iniciar y reintentar ----------

  @Test
  void startPaymentCreatesTheTransactionAndKeepsTheProviderReference() {
    when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    when(gateway.createTransaction(any(PaymentGateway.PaymentRequest.class)))
        .thenReturn(
            new PaymentGateway.PaymentSession("SBX-9", "http://localhost/pago?reference=SBX-9"));

    PaymentGateway.PaymentSession session = service.startPayment(order);

    assertThat(session.paymentUrl()).isEqualTo("http://localhost/pago?reference=SBX-9");
    verify(paymentRepository, org.mockito.Mockito.times(2)).save(any(Payment.class));
  }

  @Test
  void retryCreatesANewAttemptForAnOrderStillPendingPayment() {
    when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
    when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    when(gateway.createTransaction(any(PaymentGateway.PaymentRequest.class)))
        .thenReturn(
            new PaymentGateway.PaymentSession("SBX-2", "http://localhost/pago?reference=SBX-2"));

    assertThat(service.retry(7L, order.getAccessCode()).providerReference()).isEqualTo("SBX-2");
  }

  @Test
  void retryWithAWrongCodeLooksLikeAnOrderThatDoesNotExist() {
    when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> service.retry(7L, "CODIGO-FALSO"))
        .isInstanceOf(NotFoundException.class);
    verify(gateway, never()).createTransaction(any());
  }

  @Test
  void retryOfAnOrderThatIsNoLongerPendingPaymentIsRejected() {
    order.setStatus(OrderStatus.PENDING);
    when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
    String code = order.getAccessCode();

    assertThatThrownBy(() -> service.retry(7L, code))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El pedido ya no está pendiente de pago");
  }

  // ---------- Webhook ----------

  @Test
  void aSignedApprovedNoticeConfirmsTheOrder() {
    when(paymentRepository.findByProviderReference("SBX-1")).thenReturn(Optional.of(payment));
    String body = "{\"reference\":\"SBX-1\",\"status\":\"APPROVED\",\"amount\":24000}";

    service.handleWebhook(body, sign(body));

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    verify(confirmation).confirm(order);
  }

  @Test
  void theSamePaymentNoticeTwiceConfirmsOnlyOnce() {
    when(paymentRepository.findByProviderReference("SBX-1")).thenReturn(Optional.of(payment));
    String body = "{\"reference\":\"SBX-1\",\"status\":\"APPROVED\",\"amount\":24000}";

    service.handleWebhook(body, sign(body));
    service.handleWebhook(body, sign(body));

    verify(confirmation, org.mockito.Mockito.times(1)).confirm(order);
  }

  @Test
  void aNoticeWithoutAValidSignatureIsRejectedBeforeAnythingElse() {
    String body = "{\"reference\":\"SBX-1\",\"status\":\"APPROVED\"}";

    assertThatThrownBy(() -> service.handleWebhook(body, "firma-falsa"))
        .isInstanceOf(InvalidSignatureException.class);
    assertThatThrownBy(() -> service.handleWebhook(body, ""))
        .isInstanceOf(InvalidSignatureException.class);
    verify(paymentRepository, never()).findByProviderReference(any());
  }

  @Test
  void aTamperedBodyInvalidatesTheSignature() {
    String original = "{\"reference\":\"SBX-1\",\"status\":\"DECLINED\"}";
    String tampered = "{\"reference\":\"SBX-1\",\"status\":\"APPROVED\"}";

    assertThatThrownBy(() -> service.handleWebhook(tampered, sign(original)))
        .isInstanceOf(InvalidSignatureException.class);
  }

  @Test
  void anApprovedNoticeWithADifferentAmountIsRejected() {
    when(paymentRepository.findByProviderReference("SBX-1")).thenReturn(Optional.of(payment));
    String body = "{\"reference\":\"SBX-1\",\"status\":\"APPROVED\",\"amount\":100}";

    assertThatThrownBy(() -> service.handleWebhook(body, sign(body)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("monto");
    verify(confirmation, never()).confirm(any());
  }

  @Test
  void aDeclinedNoticeMarksThePaymentButNotTheOrder() {
    when(paymentRepository.findByProviderReference("SBX-1")).thenReturn(Optional.of(payment));
    String body = "{\"reference\":\"SBX-1\",\"status\":\"DECLINED\"}";

    service.handleWebhook(body, sign(body));

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    verify(confirmation, never()).confirm(any());
  }

  @Test
  void anUnknownReferenceIsNotFound() {
    when(paymentRepository.findByProviderReference("NO-EXISTE")).thenReturn(Optional.empty());
    String body = "{\"reference\":\"NO-EXISTE\",\"status\":\"APPROVED\"}";

    assertThatThrownBy(() -> service.handleWebhook(body, sign(body)))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void malformedNoticesAreRejected() {
    String noReference = "{\"status\":\"APPROVED\"}";
    String noStatus = "{\"reference\":\"SBX-1\"}";
    String unknownStatus = "{\"reference\":\"SBX-1\",\"status\":\"RARO\"}";
    String notJson = "esto no es json";

    assertThatThrownBy(() -> service.handleWebhook(noReference, sign(noReference)))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> service.handleWebhook(noStatus, sign(noStatus)))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> service.handleWebhook(unknownStatus, sign(unknownStatus)))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> service.handleWebhook(notJson, sign(notJson)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void aNonNumericAmountIsRejected() {
    String body = "{\"reference\":\"SBX-1\",\"status\":\"APPROVED\",\"amount\":\"mucho\"}";

    assertThatThrownBy(() -> service.handleWebhook(body, sign(body)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void anApprovedPaymentOfAnOrderThatAlreadyExpiredDoesNotReviveIt() {
    order.setStatus(OrderStatus.EXPIRED);
    when(paymentRepository.findByProviderReference("SBX-1")).thenReturn(Optional.of(payment));

    service.applyResult("SBX-1", PaymentStatus.APPROVED, null);

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
    verify(confirmation, never()).confirm(any());
  }

  // ---------- Vencimiento ----------

  @Test
  void expireStaleOrdersExpiresTheOrderAndItsPendingPayments() {
    LocalDateTime limit = NOW.minusMinutes(15);
    when(orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, limit))
        .thenReturn(List.of(order));
    when(paymentRepository.findByCustomerOrderId(7L)).thenReturn(List.of(payment));

    int expired = service.expireStaleOrders();

    assertThat(expired).isEqualTo(1);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
  }

  @Test
  void expireStaleOrdersWithNothingToExpireDoesNothing() {
    when(orderRepository.findByStatusAndCreatedAtBefore(any(), any())).thenReturn(List.of());

    assertThat(service.expireStaleOrders()).isZero();
    verify(orderRepository, never()).save(any());
  }

  @Test
  void theExpirationLimitIsComputedFromTheClock() {
    // Con el reloj fijo en NOW y 15 minutos de espera, el límite es NOW - 15 min.
    when(orderRepository.findByStatusAndCreatedAtBefore(
            OrderStatus.PENDING_PAYMENT, NOW.minusMinutes(15)))
        .thenReturn(List.of());

    service.expireStaleOrders();

    verify(orderRepository)
        .findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, NOW.minusMinutes(15));
  }
}
