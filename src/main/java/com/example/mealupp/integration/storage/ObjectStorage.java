package com.example.mealupp.integration.storage;

/**
 * Puerto hacia el almacenamiento de archivos (las fotos de los platos). Hay dos adaptadores: uno en
 * disco, para desarrollo, y uno compatible con S3, para producción.
 */
public interface ObjectStorage {

  void put(String key, byte[] data, String contentType);

  void delete(String key);

  /** Dirección pública desde la que el navegador descarga el archivo (CDN o /media). */
  String publicUrl(String key);
}
