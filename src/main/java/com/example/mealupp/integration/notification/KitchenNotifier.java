package com.example.mealupp.integration.notification;

import com.example.mealupp.dto.KitchenTicketView;
import com.example.mealupp.dto.OrderResponse;

/** Puerto para avisar en tiempo real a las pantallas de cocina, caja, entrega y administración. */
public interface KitchenNotifier {

  /** Llegó una comanda nueva a cocina. */
  void ticketCreated(KitchenTicketView ticket);

  /** Una comanda cambió (la tomaron, se canceló o se entregó). */
  void ticketUpdated(KitchenTicketView ticket);

  /** Un pedido cambió de estado o se confirmó. */
  void orderChanged(OrderResponse order);
}
