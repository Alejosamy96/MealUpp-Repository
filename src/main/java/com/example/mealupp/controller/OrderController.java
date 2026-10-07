package com.example.mealupp.controller;

import com.example.mealupp.dto.CreateOrderRequest;
import com.example.mealupp.dto.OrderResponse;
import com.example.mealupp.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint público para que el cliente haga su pedido. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return orderService.createOrder(request);
  }
}
