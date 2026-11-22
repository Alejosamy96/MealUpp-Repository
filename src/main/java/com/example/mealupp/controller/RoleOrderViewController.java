package com.example.mealupp.controller;

import com.example.mealupp.dto.CashierOrderView;
import com.example.mealupp.dto.DeliveryOrderView;
import com.example.mealupp.dto.KitchenOrderView;
import com.example.mealupp.dto.KitchenTicketView;
import com.example.mealupp.model.DeliveryZone;
import com.example.mealupp.service.KitchenTicketService;
import com.example.mealupp.service.RoleOrderViewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Una vista de pedidos por rol. Los permisos de cada ruta están en SecurityConfig. */
@RestController
@RequestMapping("/api/staff")
public class RoleOrderViewController {

  private final RoleOrderViewService viewService;
  private final KitchenTicketService ticketService;

  public RoleOrderViewController(
      RoleOrderViewService viewService, KitchenTicketService ticketService) {
    this.viewService = viewService;
    this.ticketService = ticketService;
  }

  @GetMapping("/kitchen/orders")
  public List<KitchenOrderView> kitchenOrders() {
    return viewService.kitchenOrders();
  }

  /** Pedidos de hoy que cocina ya dejó listos (y los que ya se entregaron). */
  @GetMapping("/kitchen/done")
  public List<KitchenOrderView> kitchenDone() {
    return viewService.kitchenDoneOrders();
  }

  /** Comandas pendientes. La pantalla de cocina las pide al conectarse o reconectarse. */
  @GetMapping("/kitchen/tickets")
  public List<KitchenTicketView> kitchenTickets() {
    return ticketService.pending();
  }

  @GetMapping("/cashier/orders")
  public List<CashierOrderView> cashierOrders() {
    return viewService.cashierOrders();
  }

  @GetMapping("/delivery/orders")
  public List<DeliveryOrderView> deliveryOrders(
      @RequestParam(name = "zone", required = false) DeliveryZone zone) {
    return viewService.deliveryOrders(zone);
  }

  /** Pedidos de hoy que ya se entregaron, para la pestaña «Entregados» de entrega. */
  @GetMapping("/delivery/delivered")
  public List<DeliveryOrderView> deliveredOrders(
      @RequestParam(name = "zone", required = false) DeliveryZone zone) {
    return viewService.deliveredOrders(zone);
  }
}
