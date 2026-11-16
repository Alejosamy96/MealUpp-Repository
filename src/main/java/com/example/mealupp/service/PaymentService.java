package com.example.mealupp.service;

import com.example.mealupp.config.PaymentProperties;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.InvalidSignatureException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.integration.payment.PaymentGateway;
import com.example.mealupp.model.Order;
import com.example.mealupp.model.OrderStatus;
import com.example.mealupp.model.Payment;
import com.example.mealupp.model.PaymentStatus;
import com.example.mealupp.repository.OrderRepository;
import com.example.mealupp.repository.PaymentRepository;
import com.example.mealupp.util.HmacSigner;
import com.example.mealupp.util.MiniJson;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pagos en línea: crea la transacción en la pasarela, recibe su aviso (webhook) firmado y deja el
 * pedido confirmado. Es idempotente: un aviso repetido no confirma dos veces.
 */
@Service
public class PaymentService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;
  private final PaymentGateway gateway;
  private final PaymentProperties properties;
  private final OrderConfirmationService confirmation;
  private final Clock clock;

  public PaymentService(
      PaymentRepository paymentRepository,
      OrderRepository orderRepository,
      PaymentGateway gateway,
      PaymentProperties properties,
      OrderConfirmationService confirmation,
      Clock clock) {
    this.paymentRepository = paymentRepository;
    this.orderRepository = orderRepository;
    this.gateway = gateway;
    this.properties = properties;
    this.confirmation = confirmation;
    this.clock = clock;
  }

  /** Crea un intento de pago para el pedido y devuelve la dirección donde el cliente paga. */
  @Transactional
  public PaymentGateway.PaymentSession startPayment(Order order) {
    LocalDateTime now = LocalDateTime.now(clock);
    Payment payment =
        paymentRepository.save(
            new Payment(
                order, properties.provider(), order.getTotal(), properties.currency(), now));
    PaymentGateway.PaymentSession session =
        gateway.createTransaction(
            new PaymentGateway.PaymentRequest(
                order.getId(),
                "MU-" + order.getId() + "-" + payment.getId(),
                order.getTotal(),
                properties.currency(),
                "Pedido MealUpp #" + order.getId()));
    payment.setProviderReference(session.providerReference());
    paymentRepository.save(payment);
    return session;
  }

  /**
   * Nuevo intento de pago para un pedido que sigue pendiente (por ejemplo, si el primero falló).
   */
  @Transactional
  public PaymentGateway.PaymentSession retry(Long orderId, String accessCode) {
    Order order =
        orderRepository
            .findById(orderId)
            .filter(found -> codeMatches(found.getAccessCode(), accessCode))
            .orElseThrow(() -> new NotFoundException("El pedido no existe"));
    if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
      throw new BusinessRuleException("El pedido ya no está pendiente de pago");
    }
    return startPayment(order);
  }

  /** Procesa el aviso de la pasarela. Verifica la firma antes de tocar nada. */
  @Transactional
  public void handleWebhook(String rawBody, String signature) {
    if (!HmacSigner.matches(properties.webhookSecret(), rawBody, signature)) {
      throw new InvalidSignatureException("La firma del aviso no es válida");
    }
    Map<String, String> data;
    try {
      data = MiniJson.parseFlat(rawBody);
    } catch (IllegalArgumentException ex) {
      throw new BusinessRuleException("El aviso de pago tiene un formato inválido");
    }
    String reference = data.get("reference");
    if (reference == null || reference.isBlank()) {
      throw new BusinessRuleException("El aviso de pago no trae referencia");
    }
    applyResult(reference, parseStatus(data.get("status")), parseAmount(data.get("amount")));
  }

  /**
   * Aplica el resultado de un pago. Si el pago ya tenía un resultado final no hace nada
   * (idempotente).
   *
   * @param amount monto informado por la pasarela, o nulo si no lo informa
   */
  @Transactional
  public void applyResult(String reference, PaymentStatus result, BigDecimal amount) {
    Payment payment =
        paymentRepository
            .findByProviderReference(reference)
            .orElseThrow(() -> new NotFoundException("El pago no existe"));
    if (payment.getStatus() != PaymentStatus.PENDING) {
      return;
    }
    LocalDateTime now = LocalDateTime.now(clock);
    if (result == PaymentStatus.APPROVED) {
      if (amount != null && payment.getAmount().compareTo(amount) != 0) {
        throw new BusinessRuleException("El monto del pago no coincide con el del pedido");
      }
      payment.setStatus(PaymentStatus.APPROVED, now);
      paymentRepository.save(payment);
      Order order = payment.getCustomerOrder();
      if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
        confirmation.confirm(order);
      } else {
        LOG.warn(
            "Pago aprobado de un pedido en estado {} (pedido {}): hay que revisarlo a mano",
            order.getStatus(),
            order.getId());
      }
    } else if (result == PaymentStatus.DECLINED) {
      payment.setStatus(PaymentStatus.DECLINED, now);
      paymentRepository.save(payment);
    } else {
      throw new BusinessRuleException("Resultado de pago no soportado: " + result);
    }
  }

  /** Vence los pedidos que llevan más del tiempo permitido sin pagarse. Devuelve cuántos venció. */
  @Transactional
  public int expireStaleOrders() {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime limit = now.minusMinutes(properties.expirationMinutes());
    List<Order> stale =
        orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, limit);
    for (Order order : stale) {
      order.setStatus(OrderStatus.EXPIRED);
      orderRepository.save(order);
      for (Payment payment : paymentRepository.findByCustomerOrderId(order.getId())) {
        if (payment.getStatus() == PaymentStatus.PENDING) {
          payment.setStatus(PaymentStatus.EXPIRED, now);
          paymentRepository.save(payment);
        }
      }
    }
    return stale.size();
  }

  private static PaymentStatus parseStatus(String text) {
    if (text == null) {
      throw new BusinessRuleException("El aviso de pago no trae estado");
    }
    return switch (text.trim().toUpperCase()) {
      case "APPROVED" -> PaymentStatus.APPROVED;
      case "DECLINED", "REJECTED", "ERROR", "VOIDED" -> PaymentStatus.DECLINED;
      default -> throw new BusinessRuleException("Estado de pago desconocido: " + text);
    };
  }

  private static BigDecimal parseAmount(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(text.trim());
    } catch (NumberFormatException ex) {
      throw new BusinessRuleException("El monto del aviso de pago no es un número");
    }
  }

  static boolean codeMatches(String expected, String given) {
    if (expected == null || given == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), given.trim().getBytes(StandardCharsets.UTF_8));
  }
}
