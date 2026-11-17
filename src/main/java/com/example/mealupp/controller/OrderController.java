package com.example.mealupp.controller;

import com.example.mealupp.dto.CreateOrderRequest;
import com.example.mealupp.dto.CreatedOrderResponse;
import com.example.mealupp.dto.OrderTrackingResponse;
import com.example.mealupp.dto.PaymentLinkResponse;
import com.example.mealupp.service.OrderService;
import com.example.mealupp.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints públicos para que el cliente haga su pedido, lo consulte y pague. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final OrderService orderService;
  private final PaymentService paymentService;

  public OrderController(OrderService orderService, PaymentService paymentService) {
    this.orderService = orderService;
    this.paymentService = paymentService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreatedOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return orderService.createOrder(request);
  }

  /** Consulta el pedido con el código de acceso que se entregó al crearlo. */
  @GetMapping("/{id}")
  public OrderTrackingResponse track(
      @PathVariable("id") Long id, @RequestParam("code") String code) {
    return orderService.track(id, code);
  }

  /** Nuevo intento de pago de un pedido que sigue pendiente. */
  @PostMapping("/{id}/pay")
  public PaymentLinkResponse pay(@PathVariable("id") Long id, @RequestParam("code") String code) {
    return new PaymentLinkResponse(paymentService.retry(id, code).paymentUrl());
  }
}
