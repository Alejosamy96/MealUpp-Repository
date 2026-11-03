package com.example.mealupp.controller;

import com.example.mealupp.dto.CashierOrderView;
import com.example.mealupp.dto.KitchenOrderView;
import com.example.mealupp.service.RoleOrderViewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU-06: una vista de pedidos por rol. Los permisos de cada ruta están en SecurityConfig. */
@RestController
@RequestMapping("/api/staff")
public class RoleOrderViewController {

  private final RoleOrderViewService viewService;

  public RoleOrderViewController(RoleOrderViewService viewService) {
    this.viewService = viewService;
  }

  @GetMapping("/kitchen/orders")
  public List<KitchenOrderView> kitchenOrders() {
    return viewService.kitchenOrders();
  }

  @GetMapping("/cashier/orders")
  public List<CashierOrderView> cashierOrders() {
    return viewService.cashierOrders();
  }
}
