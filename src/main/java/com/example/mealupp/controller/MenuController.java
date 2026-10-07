package com.example.mealupp.controller;

import com.example.mealupp.dto.MenuResponse;
import com.example.mealupp.service.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

  private final MenuService menuService;

  public MenuController(MenuService menuService) {
    this.menuService = menuService;
  }

  @GetMapping
  public MenuResponse getMenu() {
    return menuService.getMenu();
  }
}
