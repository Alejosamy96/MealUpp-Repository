package com.example.mealupp.controller;

import com.example.mealupp.dto.CreateDishRequest;
import com.example.mealupp.dto.StaffDishResponse;
import com.example.mealupp.dto.UpdateDishAvailabilityRequest;
import com.example.mealupp.dto.UpdateDishRequest;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.service.DishAdminService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Administración del menú y de las fotos de los platos. Solo para el rol ADMIN. */
@RestController
@RequestMapping("/api/staff/dishes")
public class StaffDishController {

  private final DishAdminService dishAdminService;

  public StaffDishController(DishAdminService dishAdminService) {
    this.dishAdminService = dishAdminService;
  }

  @GetMapping
  public List<StaffDishResponse> listAll() {
    return dishAdminService.listAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public StaffDishResponse create(@Valid @RequestBody CreateDishRequest request) {
    return dishAdminService.create(request);
  }

  @PutMapping("/{id}")
  public StaffDishResponse update(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateDishRequest request) {
    return dishAdminService.update(id, request);
  }

  @PatchMapping("/{id}/availability")
  public StaffDishResponse updateAvailability(
      @PathVariable("id") Long id, @Valid @RequestBody UpdateDishAvailabilityRequest request) {
    return dishAdminService.updateAvailability(id, request.available());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    dishAdminService.delete(id);
  }

  /** Sube la foto del plato como multipart/form-data, en el campo "file" (JPG o PNG, máx. 5 MB). */
  @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public StaffDishResponse uploadPhoto(
      @PathVariable("id") Long id, @RequestParam("file") MultipartFile file) {
    try {
      return dishAdminService.uploadPhoto(id, file.getBytes());
    } catch (IOException ex) {
      throw new BusinessRuleException("No se pudo leer la imagen");
    }
  }

  @DeleteMapping("/{id}/photo")
  public StaffDishResponse deletePhoto(@PathVariable("id") Long id) {
    return dishAdminService.deletePhoto(id);
  }
}
