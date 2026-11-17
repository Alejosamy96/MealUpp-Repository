package com.example.mealupp.service;

import com.example.mealupp.integration.storage.ObjectStorage;
import org.springframework.stereotype.Component;

/** Convierte la clave de la foto de un plato en las direcciones públicas de sus dos versiones. */
@Component
public class MediaUrls {

  public static final String THUMBNAIL_SUFFIX = "_thumb.jpg";
  public static final String DETAIL_SUFFIX = "_detail.jpg";

  private final ObjectStorage storage;

  public MediaUrls(ObjectStorage storage) {
    this.storage = storage;
  }

  public String thumbnailUrl(String imageKey) {
    return imageKey == null ? null : storage.publicUrl(imageKey + THUMBNAIL_SUFFIX);
  }

  public String detailUrl(String imageKey) {
    return imageKey == null ? null : storage.publicUrl(imageKey + DETAIL_SUFFIX);
  }
}
