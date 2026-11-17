package com.example.mealupp.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Sirve las fotos guardadas en disco en la ruta /media. Solo aplica al almacenamiento local. */
@Configuration
@ConditionalOnProperty(name = "mealup.storage.type", havingValue = "local", matchIfMissing = true)
public class MediaWebConfig implements WebMvcConfigurer {

  private final Path directory;

  public MediaWebConfig(@Value("${mealup.storage.local-dir}") String directory) {
    this.directory = Path.of(directory).toAbsolutePath().normalize();
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    try {
      Files.createDirectories(directory);
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo crear la carpeta de fotos " + directory, ex);
    }
    String location = directory.toUri().toString();
    registry
        .addResourceHandler("/media/**")
        .addResourceLocations(location.endsWith("/") ? location : location + "/")
        .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());
  }
}
