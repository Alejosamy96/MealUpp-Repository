package com.example.mealupp.dto;

import com.example.mealupp.model.Order;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Un plato de un pedido y cuántos almuerzos de ese plato. Sin precios. */
public record OrderItemView(String dishName, int quantity) {

  public static List<OrderItemView> listOf(Order order) {
    return order.lines().stream()
        .map(line -> new OrderItemView(line.dish().getName(), line.quantity()))
        .toList();
  }

  /** Forma sencilla (mapa) para el mensaje que se publica por WebSocket. */
  public static List<Map<String, Object>> asMaps(List<OrderItemView> items) {
    return items.stream()
        .map(
            item -> {
              Map<String, Object> map = new LinkedHashMap<>();
              map.put("dishName", item.dishName());
              map.put("quantity", item.quantity());
              return map;
            })
        .toList();
  }
}
