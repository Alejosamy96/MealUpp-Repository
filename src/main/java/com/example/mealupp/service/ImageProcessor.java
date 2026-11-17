package com.example.mealupp.service;

import com.example.mealupp.exception.BusinessRuleException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * Valida la foto de un plato y genera dos versiones en JPEG: una miniatura para el listado y una
 * imagen de detalle. Solo acepta JPG y PNG, de máximo 5 MB.
 */
public final class ImageProcessor {

  public static final long MAX_BYTES = 5L * 1024 * 1024;
  private static final long MAX_PIXELS = 40_000_000L;
  private static final float JPEG_QUALITY = 0.85f;

  /** Las dos versiones ya codificadas. */
  public record ProcessedImage(byte[] thumbnail, byte[] detail) {

    @Override
    public boolean equals(Object other) {
      return other instanceof ProcessedImage that
          && Arrays.equals(thumbnail, that.thumbnail)
          && Arrays.equals(detail, that.detail);
    }

    @Override
    public int hashCode() {
      return 31 * Arrays.hashCode(thumbnail) + Arrays.hashCode(detail);
    }

    @Override
    public String toString() {
      return "ProcessedImage[thumbnail="
          + thumbnail.length
          + " bytes, detail="
          + detail.length
          + " bytes]";
    }
  }

  private final int thumbnailSize;
  private final int detailSize;

  public ImageProcessor(int thumbnailSize, int detailSize) {
    this.thumbnailSize = thumbnailSize;
    this.detailSize = detailSize;
  }

  public ProcessedImage process(byte[] data) {
    if (data == null || data.length == 0) {
      throw new BusinessRuleException("La imagen está vacía");
    }
    if (data.length > MAX_BYTES) {
      throw new BusinessRuleException("La imagen supera el tamaño máximo de 5 MB");
    }
    BufferedImage image = read(data);
    return new ProcessedImage(
        encode(scale(image, thumbnailSize)), encode(scale(image, detailSize)));
  }

  private static BufferedImage read(byte[] data) {
    try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
      if (input == null) {
        throw invalid();
      }
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw invalid();
      }
      ImageReader reader = readers.next();
      try {
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png")) {
          throw invalid();
        }
        reader.setInput(input);
        if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
          throw new BusinessRuleException("La imagen tiene demasiados píxeles");
        }
        BufferedImage image = reader.read(0);
        if (image == null) {
          throw invalid();
        }
        return image;
      } finally {
        reader.dispose();
      }
    } catch (IOException | RuntimeException ex) {
      if (ex instanceof BusinessRuleException business) {
        throw business;
      }
      throw invalid();
    }
  }

  private static BusinessRuleException invalid() {
    return new BusinessRuleException("El archivo no es una imagen JPG o PNG válida");
  }

  /** Reduce la imagen para que su lado mayor no pase de "max"; nunca la agranda. */
  private static BufferedImage scale(BufferedImage source, int max) {
    int width = source.getWidth();
    int height = source.getHeight();
    double factor = Math.min(1.0, (double) max / Math.max(width, height));
    int newWidth = Math.max(1, (int) Math.round(width * factor));
    int newHeight = Math.max(1, (int) Math.round(height * factor));
    BufferedImage out = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = out.createGraphics();
    try {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, newWidth, newHeight);
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.drawImage(source, 0, 0, newWidth, newHeight, null);
    } finally {
      g.dispose();
    }
    return out;
  }

  private static byte[] encode(BufferedImage image) {
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(JPEG_QUALITY);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
      writer.setOutput(output);
      writer.write(null, new IIOImage(image, null, null), param);
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo codificar la imagen", ex);
    } finally {
      writer.dispose();
    }
    return bytes.toByteArray();
  }
}
