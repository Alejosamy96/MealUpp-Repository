package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mealupp.dto.CreateDishRequest;
import com.example.mealupp.dto.StaffDishResponse;
import com.example.mealupp.dto.UpdateDishRequest;
import com.example.mealupp.exception.BusinessRuleException;
import com.example.mealupp.exception.NotFoundException;
import com.example.mealupp.integration.storage.ObjectStorage;
import com.example.mealupp.model.Dish;
import com.example.mealupp.repository.DishRepository;
import com.example.mealupp.repository.OrderRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishAdminServiceTest {

  @Mock private DishRepository dishRepository;
  @Mock private OrderRepository orderRepository;
  @Mock private ObjectStorage storage;

  private DishAdminService service;
  private Dish dish;

  @BeforeEach
  void setUp() {
    service =
        new DishAdminService(
            dishRepository,
            orderRepository,
            storage,
            new ImageProcessor(400, 1200),
            new MediaUrls(storage),
            java.time.Clock.fixed(
                java.time.Instant.parse("2026-11-23T15:00:00Z"),
                java.time.ZoneId.of("America/Bogota")));
    dish = new Dish("Pollo a la plancha", "Con arroz", new BigDecimal("12000"), true);
  }

  private void saveReturnsTheSameDish() {
    when(dishRepository.save(any(Dish.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  private static byte[] png() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB), "png", out);
    return out.toByteArray();
  }

  // ---------- Crear ----------

  @Test
  void createSavesADishThatIsAvailableByDefault() {
    saveReturnsTheSameDish();

    StaffDishResponse response =
        service.create(
            new CreateDishRequest(
                "  Pasta Alfredo ", " Con pollo ", new BigDecimal("11000"), null));

    assertThat(response.name()).isEqualTo("Pasta Alfredo");
    assertThat(response.description()).isEqualTo("Con pollo");
    assertThat(response.available()).isTrue();
    assertThat(response.thumbnailUrl()).isNull();
  }

  @Test
  void createCanSaveADishAsUnavailable() {
    saveReturnsTheSameDish();

    StaffDishResponse response =
        service.create(new CreateDishRequest("Pasta", null, new BigDecimal("11000"), false));

    assertThat(response.available()).isFalse();
    assertThat(response.description()).isNull();
  }

  @Test
  void createRejectsADuplicateNameIgnoringCase() {
    when(dishRepository.existsByNameIgnoreCase("pasta")).thenReturn(true);
    CreateDishRequest request = new CreateDishRequest("pasta", null, new BigDecimal("11000"), true);

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Ya existe un plato con ese nombre");
    verify(dishRepository, never()).save(any());
  }

  @Test
  void createRejectsPricesThatAreNotPositive() {
    CreateDishRequest zero = new CreateDishRequest("Pasta", null, BigDecimal.ZERO, true);
    CreateDishRequest negative = new CreateDishRequest("Pasta", null, new BigDecimal("-1"), true);

    assertThatThrownBy(() -> service.create(zero))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El precio debe ser mayor que 0");
    assertThatThrownBy(() -> service.create(negative)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void createAcceptsANameOf80CharactersAndRejectsOne81() {
    saveReturnsTheSameDish();
    CreateDishRequest ok = new CreateDishRequest("a".repeat(80), null, BigDecimal.ONE, true);
    CreateDishRequest tooLong = new CreateDishRequest("a".repeat(81), null, BigDecimal.ONE, true);

    assertThat(service.create(ok).name()).hasSize(80);
    assertThatThrownBy(() -> service.create(tooLong))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("entre 1 y 80");
  }

  @Test
  void createRejectsABlankName() {
    CreateDishRequest blank = new CreateDishRequest("   ", null, BigDecimal.ONE, true);

    assertThatThrownBy(() -> service.create(blank)).isInstanceOf(BusinessRuleException.class);
  }

  // ---------- Editar, disponibilidad y borrar ----------

  @Test
  void updateChangesNameDescriptionAndPrice() {
    when(dishRepository.findById(1L)).thenReturn(Optional.of(dish));
    saveReturnsTheSameDish();

    StaffDishResponse response =
        service.update(
            1L, new UpdateDishRequest("Pollo asado", "Con papas", new BigDecimal("13000")));

    assertThat(response.name()).isEqualTo("Pollo asado");
    assertThat(response.price()).isEqualByComparingTo("13000");
  }

  @Test
  void updateRejectsTheNameOfAnotherDish() {
    when(dishRepository.findById(1L)).thenReturn(Optional.of(dish));
    when(dishRepository.existsByNameIgnoreCaseAndIdNot("Pasta", 1L)).thenReturn(true);
    UpdateDishRequest request = new UpdateDishRequest("Pasta", null, BigDecimal.ONE);

    assertThatThrownBy(() -> service.update(1L, request)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void updateOfAnUnknownDishIsNotFound() {
    when(dishRepository.findById(9L)).thenReturn(Optional.empty());
    UpdateDishRequest request = new UpdateDishRequest("Pasta", null, BigDecimal.ONE);

    assertThatThrownBy(() -> service.update(9L, request)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void availabilityCanBeTurnedOffAndOn() {
    when(dishRepository.findById(1L)).thenReturn(Optional.of(dish));
    saveReturnsTheSameDish();

    assertThat(service.updateAvailability(1L, false).available()).isFalse();
    assertThat(service.updateAvailability(1L, true).available()).isTrue();
  }

  @Test
  void aDishWithOrdersCannotBeDeleted() {
    when(dishRepository.findById(1L)).thenReturn(Optional.of(dish));
    when(orderRepository.existsByDishId(1L)).thenReturn(true);

    assertThatThrownBy(() -> service.delete(1L))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("márcalo como agotado");
    verify(dishRepository, never()).delete(any(Dish.class));
  }

  @Test
  void deletingADishAlsoDeletesItsPhotoFiles() {
    dish.setImageKey("dishes/1/ab12cd");
    when(dishRepository.findById(1L)).thenReturn(Optional.of(dish));
    when(orderRepository.existsByDishId(1L)).thenReturn(false);

    service.delete(1L);

    verify(storage).delete("dishes/1/ab12cd_thumb.jpg");
    verify(storage).delete("dishes/1/ab12cd_detail.jpg");
    verify(dishRepository).delete(dish);
  }

  @Test
  void listShowsEveryDishIncludingTheUnavailableOnes() {
    Dish hidden = new Dish("Agotado", null, BigDecimal.ONE, false);
    when(dishRepository.findAllByOrderByNameAsc()).thenReturn(List.of(hidden, dish));

    assertThat(service.listAll())
        .extracting(StaffDishResponse::available)
        .containsExactly(false, true);
  }

  // ---------- Fotos ----------

  @Test
  void uploadStoresTheTwoVersionsAndKeepsOnlyTheKeyInTheDatabase() throws IOException {
    when(dishRepository.findById(1L)).thenReturn(Optional.of(dish));
    saveReturnsTheSameDish();

    service.uploadPhoto(1L, png());

    ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
    verify(storage, org.mockito.Mockito.times(2))
        .put(keys.capture(), any(byte[].class), eq("image/jpeg"));
    assertThat(keys.getAllValues().get(0)).matches("dishes/1/[0-9a-f]{12}_thumb\\.jpg");
    assertThat(keys.getAllValues().get(1)).matches("dishes/1/[0-9a-f]{12}_detail\\.jpg");
    assertThat(dish.getImageKey()).matches("dishes/1/[0-9a-f]{12}");
  }

  @Test
  void replacingThePhotoDeletesTheOldFiles() throws IOException {
    dish.setImageKey("dishes/1/viejo00000000");
    when(dishRepository.findById(1L)).thenReturn(Optional.of(dish));
    saveReturnsTheSameDish();

    service.uploadPhoto(1L, png());

    verify(storage).delete("dishes/1/viejo00000000_thumb.jpg");
    verify(storage).delete("dishes/1/viejo00000000_detail.jpg");
    assertThat(dish.getImageKey()).doesNotContain("viejo");
  }

  @Test
  void anInvalidImageIsRejectedAndNothingIsStored() {
    when(dishRepository.findById(1L)).thenReturn(Optional.of(dish));

    assertThatThrownBy(() -> service.uploadPhoto(1L, "texto".getBytes()))
        .isInstanceOf(BusinessRuleException.class);
    verify(storage, never()).put(anyString(), any(byte[].class), anyString());
    verify(dishRepository, never()).save(any());
  }

  @Test
  void deletePhotoClearsTheKeyAndTheFiles() {
    dish.setImageKey("dishes/1/ab12cd");
    when(dishRepository.findById(1L)).thenReturn(Optional.of(dish));
    saveReturnsTheSameDish();

    StaffDishResponse response = service.deletePhoto(1L);

    assertThat(response.imageUrl()).isNull();
    assertThat(dish.getImageKey()).isNull();
    verify(storage).delete("dishes/1/ab12cd_thumb.jpg");
  }

  @Test
  void aStorageFailureWhileDeletingOldFilesDoesNotBreakTheUpdate() {
    dish.setImageKey("dishes/1/ab12cd");
    when(dishRepository.findById(1L)).thenReturn(Optional.of(dish));
    saveReturnsTheSameDish();
    org.mockito.Mockito.doThrow(new IllegalStateException("sin red"))
        .when(storage)
        .delete(anyString());

    StaffDishResponse response = service.deletePhoto(1L);

    assertThat(response.imageUrl()).isNull();
  }
}
