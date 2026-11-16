package com.example.mealupp.controller;

import com.example.mealupp.dto.DailyOrdersResponse;
import com.example.mealupp.dto.OrderResponse;
import com.example.mealupp.dto.UpdateOrderStatusRequest;
import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.security.StaffPrincipal;
import com.example.mealupp.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pedidos para el personal. Ver todos los pedidos es solo de ADMIN. Cambiar el estado lo puede
 * intentar cualquier rol, pero OrderService revisa qué cambio le corresponde a cada uno.
 */
@RestController
@RequestMapping("/api/staff/orders")
public class StaffOrderController {

  private final OrderService orderService;

  public StaffOrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping
  public DailyOrdersResponse getDailyOrders(
      @RequestParam(name = "zone", required = false) DeliveryZone zone) {
    return orderService.getDailyOrders(zone);
  }

  @PatchMapping("/{id}/status")
  public OrderResponse updateStatus(
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateOrderStatusRequest request,
      @AuthenticationPrincipal StaffPrincipal principal) {
    return orderService.updateStatus(id, request.status(), principal.role(), principal.username());
  }
}
