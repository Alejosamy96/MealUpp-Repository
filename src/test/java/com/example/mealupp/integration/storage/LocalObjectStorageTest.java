package com.example.mealupp.integration.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalObjectStorageTest {

  @TempDir Path directory;

  @Test
  void savesAndDeletesFilesInsideTheFolder() {
    LocalObjectStorage storage = new LocalObjectStorage(directory, "/media/");

    storage.put("dishes/2/a.jpg", new byte[] {9}, "image/jpeg");
    assertThat(directory.resolve("dishes/2/a.jpg")).exists();

    storage.delete("dishes/2/a.jpg");
    assertThat(directory.resolve("dishes/2/a.jpg")).doesNotExist();
  }

  @Test
  void deletingAMissingFileIsNotAnError() {
    new LocalObjectStorage(directory, "/media").delete("dishes/9/no-existe.jpg");

    assertThat(Files.exists(directory.resolve("dishes/9/no-existe.jpg"))).isFalse();
  }

  @Test
  void publicUrlJoinsTheBaseAddressAndTheKey() {
    assertThat(new LocalObjectStorage(directory, "/media/").publicUrl("dishes/2/a.jpg"))
        .isEqualTo("/media/dishes/2/a.jpg");
    assertThat(new LocalObjectStorage(directory, "https://cdn.x.com").publicUrl("k.jpg"))
        .isEqualTo("https://cdn.x.com/k.jpg");
  }

  @Test
  void aKeyCannotLeaveTheFolder() {
    LocalObjectStorage storage = new LocalObjectStorage(directory, "/media");

    assertThatThrownBy(() -> storage.put("../fuera.txt", new byte[] {1}, "text/plain"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> storage.delete("../../etc/passwd"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
