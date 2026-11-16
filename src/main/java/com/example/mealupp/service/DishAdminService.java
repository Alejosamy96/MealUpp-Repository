package com.example.mealupp.service;

import com.example.mealupp.dto.CreateDishRequest;
import com.example.mealupp.dto.StaffDishResponse;
import com.example.mealupp.dto.UpdateDishRequest;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.integration.storage.ObjectStorage;
import com.example.mealupp.model.Dish;
import com.example.mealupp.repository.DishRepository;
import com.example.mealupp.repository.OrderRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** La administradora gestiona los platos del menú y sus fotos. */
@Service
public class DishAdminService {

  static final int MAX_NAME_LENGTH = 80;

  private static final Logger LOG = LoggerFactory.getLogger(DishAdminService.class);

  private final DishRepository dishRepository;
  private final OrderRepository orderRepository;
  private final ObjectStorage storage;
  private final ImageProcessor imageProcessor;
  private final MediaUrls mediaUrls;
  private final SecureRandom random = new SecureRandom();

  public DishAdminService(
      DishRepository dishRepository,
      OrderRepository orderRepository,
      ObjectStorage storage,
      ImageProcessor imageProcessor,
      MediaUrls mediaUrls) {
    this.dishRepository = dishRepository;
    this.orderRepository = orderRepository;
    this.storage = storage;
    this.imageProcessor = imageProcessor;
    this.mediaUrls = mediaUrls;
  }

  @Transactional(readOnly = true)
  public List<StaffDishResponse> listAll() {
    return dishRepository.findAllByOrderByNameAsc().stream()
        .map(dish -> StaffDishResponse.from(dish, mediaUrls))
        .toList();
  }

  @Transactional
  public StaffDishResponse create(CreateDishRequest request) {
    String name = request.name().trim();
    validateName(name);
    if (dishRepository.existsByNameIgnoreCase(name)) {
      throw new BusinessRuleException("Ya existe un plato con ese nombre");
    }
    validatePrice(request.price());
    boolean available = request.available() == null || request.available();
    Dish dish = new Dish(name, clean(request.description()), request.price(), available);
    return StaffDishResponse.from(dishRepository.save(dish), mediaUrls);
  }

  @Transactional
  public StaffDishResponse update(Long id, UpdateDishRequest request) {
    Dish dish = find(id);
    String name = request.name().trim();
    validateName(name);
    if (dishRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
      throw new BusinessRuleException("Ya existe un plato con ese nombre");
    }
    validatePrice(request.price());
    dish.setName(name);
    dish.setDescription(clean(request.description()));
    dish.setPrice(request.price());
    return StaffDishResponse.from(dishRepository.save(dish), mediaUrls);
  }

  @Transactional
  public StaffDishResponse updateAvailability(Long id, boolean available) {
    Dish dish = find(id);
    dish.setAvailable(available);
    return StaffDishResponse.from(dishRepository.save(dish), mediaUrls);
  }

  /** Elimina un plato. Si ya tiene pedidos no se puede borrar: se marca como agotado. */
  @Transactional
  public void delete(Long id) {
    Dish dish = find(id);
    if (orderRepository.existsByDishId(id)) {
      throw new BusinessRuleException(
          "No se puede eliminar un plato que ya tiene pedidos; márcalo como agotado");
    }
    deleteFiles(dish.getImageKey());
    dishRepository.delete(dish);
  }

  /** Guarda la foto del plato (reemplaza la anterior, si había). */
  @Transactional
  public StaffDishResponse uploadPhoto(Long id, byte[] data) {
    Dish dish = find(id);
    ImageProcessor.ProcessedImage image = imageProcessor.process(data);
    String baseKey = "dishes/" + id + "/" + newToken();
    storage.put(baseKey + MediaUrls.THUMBNAIL_SUFFIX, image.thumbnail(), "image/jpeg");
    storage.put(baseKey + MediaUrls.DETAIL_SUFFIX, image.detail(), "image/jpeg");
    String previous = dish.getImageKey();
    dish.setImageKey(baseKey);
    Dish saved = dishRepository.save(dish);
    deleteFiles(previous);
    return StaffDishResponse.from(saved, mediaUrls);
  }

  @Transactional
  public StaffDishResponse deletePhoto(Long id) {
    Dish dish = find(id);
    String previous = dish.getImageKey();
    dish.setImageKey(null);
    Dish saved = dishRepository.save(dish);
    deleteFiles(previous);
    return StaffDishResponse.from(saved, mediaUrls);
  }

  private Dish find(Long id) {
    return dishRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("El plato no existe"));
  }

  private void deleteFiles(String imageKey) {
    if (imageKey == null) {
      return;
    }
    for (String suffix : List.of(MediaUrls.THUMBNAIL_SUFFIX, MediaUrls.DETAIL_SUFFIX)) {
      try {
        storage.delete(imageKey + suffix);
      } catch (RuntimeException ex) {
        LOG.warn("No se pudo borrar {}{}: {}", imageKey, suffix, ex.getMessage());
      }
    }
  }

  private String newToken() {
    byte[] bytes = new byte[6];
    random.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private static void validateName(String name) {
    if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
      throw new BusinessRuleException(
          "El nombre del plato debe tener entre 1 y " + MAX_NAME_LENGTH + " caracteres");
    }
  }

  private static void validatePrice(BigDecimal price) {
    if (price.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessRuleException("El precio debe ser mayor que 0");
    }
  }

  private static String clean(String text) {
    return text == null || text.isBlank() ? null : text.trim();
  }
}
