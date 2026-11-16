package com.example.mealupp.integration.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Guarda los archivos en una carpeta del servidor. Pensado para desarrollo y pruebas. */
public final class LocalObjectStorage implements ObjectStorage {

  private final Path root;
  private final String baseUrl;

  public LocalObjectStorage(Path root, String baseUrl) {
    this.root = root.toAbsolutePath().normalize();
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  @Override
  public void put(String key, byte[] data, String contentType) {
    Path target = resolve(key);
    try {
      Files.createDirectories(target.getParent());
      Files.write(target, data);
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo guardar el archivo " + key, ex);
    }
  }

  @Override
  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo borrar el archivo " + key, ex);
    }
  }

  @Override
  public String publicUrl(String key) {
    return baseUrl + "/" + key;
  }

  /** Evita que una clave como "../x" salga de la carpeta raíz. */
  private Path resolve(String key) {
    Path target = root.resolve(key).normalize();
    if (!target.startsWith(root)) {
      throw new IllegalArgumentException("Clave de archivo inválida: " + key);
    }
    return target;
  }
}
