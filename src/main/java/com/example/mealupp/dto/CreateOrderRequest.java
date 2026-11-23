package com.example.mealupp.dto;

import com.example.mealupp.model.DeliveryZone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

/**
 * Pedido nuevo. Se pueden pedir varios platos con "items". Para un solo plato también sirven
 * "dishId" y "quantity" (así funcionaba antes); si llegan "items", se usan esos. El detalle de
 * entrega es opcional en el centro comercial y obligatorio en la calle de los muebles.
 */
public record CreateOrderRequest(
    Long dishId,
    Integer quantity,
    @Valid List<OrderItemRequest> items,
    @NotBlank String customerName,
    @NotNull LocalTime deliveryTime,
    @NotNull DeliveryZone deliveryZone,
    String deliveryDetail) {

  /** Platos pedidos, venga la solicitud con "items" o con "dishId" y "quantity". */
  public List<OrderItemRequest> lines() {
    if (items != null && !items.isEmpty()) {
      return items;
    }
    if (dishId != null && quantity != null) {
      return List.of(new OrderItemRequest(dishId, quantity));
    }
    return List.of();
  }
}
