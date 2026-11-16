package com.example.mealupp.config;

import com.example.mealupp.integration.storage.LocalObjectStorage;
import com.example.mealupp.integration.storage.ObjectStorage;
import com.example.mealupp.integration.storage.S3ObjectStorage;
import com.example.mealupp.service.ImageProcessor;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dónde se guardan las fotos. mealup.storage.type=local las guarda en una carpeta y las sirve en
 * /media (desarrollo); mealup.storage.type=s3 las sube a un almacenamiento compatible con S3 y las
 * sirve por su CDN (producción).
 */
@Configuration
public class StorageConfig {

  @Bean
  @ConditionalOnProperty(name = "mealup.storage.type", havingValue = "local", matchIfMissing = true)
  public ObjectStorage localObjectStorage(
      @Value("${mealup.storage.local-dir}") String directory,
      @Value("${mealup.media.base-url}") String baseUrl) {
    return new LocalObjectStorage(Path.of(directory), baseUrl);
  }

  @Bean
  @ConditionalOnProperty(name = "mealup.storage.type", havingValue = "s3")
  public ObjectStorage s3ObjectStorage(
      @Value("${mealup.storage.s3.endpoint}") String endpoint,
      @Value("${mealup.storage.s3.bucket}") String bucket,
      @Value("${mealup.storage.s3.region}") String region,
      @Value("${mealup.storage.s3.access-key}") String accessKey,
      @Value("${mealup.storage.s3.secret-key}") String secretKey,
      @Value("${mealup.media.base-url}") String publicBaseUrl,
      Clock clock) {
    return new S3ObjectStorage(
        endpoint, bucket, region, accessKey, secretKey, publicBaseUrl, clock);
  }

  @Bean
  public ImageProcessor imageProcessor(
      @Value("${mealup.media.thumbnail-size}") int thumbnailSize,
      @Value("${mealup.media.detail-size}") int detailSize) {
    return new ImageProcessor(thumbnailSize, detailSize);
  }
}
