package com.example.mealupp.service;

import com.example.mealupp.config.MercadoPagoProperties;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.InvalidSignatureException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.integration.payment.MercadoPagoClient;
import com.example.mealupp.integration.payment.MercadoPagoSignature;
import com.example.mealupp.model.PaymentStatus;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Procesa los avisos (webhooks) de Mercado Pago. Verifica la firma, consulta el pago real en la API
 * de Mercado Pago (no se confía en lo que diga el aviso) y aplica el resultado al pedido.
 */
@Service
@ConditionalOnProperty(name = "mealup.payments.provider", havingValue = "mercadopago")
public class MercadoPagoNotificationService {

  private static final Logger LOG = LoggerFactory.getLogger(MercadoPagoNotificationService.class);

  private final MercadoPagoClient client;
  private final MercadoPagoProperties properties;
  private final PaymentService paymentService;

  public MercadoPagoNotificationService(
      MercadoPagoClient client, MercadoPagoProperties properties, PaymentService paymentService) {
    this.client = client;
    this.properties = properties;
    this.paymentService = paymentService;
  }

  /**
   * @param dataId id del recurso (data.id)
   * @param type tipo de aviso; solo se atienden los de "payment"
   * @param signature encabezado x-signature
   * @param requestId encabezado x-request-id
   */
  public void handle(String dataId, String type, String signature, String requestId) {
    if (!MercadoPagoSignature.isValid(properties.webhookSecret(), signature, requestId, dataId)) {
      throw new InvalidSignatureException("La firma del aviso no es válida");
    }
    if (!"payment".equals(type) || dataId == null || dataId.isBlank()) {
      return;
    }
    Optional<Map<String, Object>> found = client.findPayment(dataId);
    if (found.isEmpty()) {
      LOG.info("Mercado Pago avisó de un pago que no existe; se ignora");
      return;
    }
    Map<String, Object> payment = found.get();
    PaymentStatus result = toStatus(payment.get("status"));
    if (result == null) {
      return;
    }
    Object reference = payment.get("external_reference");
    if (!(reference instanceof String text) || text.isBlank()) {
      LOG.info("Un pago de Mercado Pago no trae referencia; se ignora");
      return;
    }
    try {
      paymentService.applyResult(text, result, toAmount(payment.get("transaction_amount")));
    } catch (NotFoundException ex) {
      LOG.info("Un pago de Mercado Pago no corresponde a ningún pedido; se ignora");
    }
  }

  /** Aprobado y rechazado son finales. Pendiente, en proceso, reembolsado, etc. se ignoran. */
  static PaymentStatus toStatus(Object status) {
    if (!(status instanceof String text)) {
      return null;
    }
    return switch (text) {
      case "approved" -> PaymentStatus.APPROVED;
      case "rejected", "cancelled" -> PaymentStatus.DECLINED;
      default -> null;
    };
  }

  private static BigDecimal toAmount(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return new BigDecimal(value.toString());
    } catch (NumberFormatException ex) {
      throw new BusinessRuleException("El monto del pago de Mercado Pago no es un número");
    }
  }
}
