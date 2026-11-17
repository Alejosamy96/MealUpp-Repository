package com.example.mealupp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.mealupp.exception.BusinessRuleException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageProcessorTest {

  private final ImageProcessor processor = new ImageProcessor(400, 1200);

  private static byte[] image(int width, int height, int type, String format) throws IOException {
    BufferedImage image = new BufferedImage(width, height, type);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        image.setRGB(x, y, (x % 2 == 0) ? 0xFFC96F4A : 0x80FFFFFF);
      }
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, format, out);
    return out.toByteArray();
  }

  private static BufferedImage decode(byte[] bytes) throws IOException {
    return ImageIO.read(new ByteArrayInputStream(bytes));
  }

  @Test
  void aBigPngBecomesTwoSmallerJpegsKeepingTheProportion() throws IOException {
    ImageProcessor.ProcessedImage result =
        processor.process(image(2000, 1000, BufferedImage.TYPE_INT_ARGB, "png"));

    BufferedImage thumbnail = decode(result.thumbnail());
    BufferedImage detail = decode(result.detail());
    assertThat(thumbnail.getWidth()).isEqualTo(400);
    assertThat(thumbnail.getHeight()).isEqualTo(200);
    assertThat(detail.getWidth()).isEqualTo(1200);
    assertThat(detail.getHeight()).isEqualTo(600);
  }

  @Test
  void aSmallImageIsNeverEnlarged() throws IOException {
    ImageProcessor.ProcessedImage result =
        processor.process(image(100, 50, BufferedImage.TYPE_INT_RGB, "jpg"));

    BufferedImage thumbnail = decode(result.thumbnail());
    assertThat(thumbnail.getWidth()).isEqualTo(100);
    assertThat(thumbnail.getHeight()).isEqualTo(50);
  }

  @Test
  void aTallImageIsLimitedByItsHeight() throws IOException {
    ImageProcessor.ProcessedImage result =
        processor.process(image(500, 2000, BufferedImage.TYPE_INT_RGB, "png"));

    BufferedImage thumbnail = decode(result.thumbnail());
    assertThat(thumbnail.getHeight()).isEqualTo(400);
    assertThat(thumbnail.getWidth()).isEqualTo(100);
  }

  @Test
  void textThatIsNotAnImageIsRejected() {
    assertThatThrownBy(() -> processor.process("no soy una imagen".getBytes()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El archivo no es una imagen JPG o PNG válida");
  }

  @Test
  void anEmptyFileIsRejected() {
    assertThatThrownBy(() -> processor.process(new byte[0]))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La imagen está vacía");
    assertThatThrownBy(() -> processor.process(null)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void aFileOver5MbIsRejectedAndOneAtTheLimitIsNotCutOff() {
    assertThatThrownBy(() -> processor.process(new byte[(int) ImageProcessor.MAX_BYTES + 1]))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("La imagen supera el tamaño máximo de 5 MB");
  }

  @Test
  void aGifIsRejectedBecauseOnlyJpgAndPngAreAccepted() throws IOException {
    assertThatThrownBy(() -> processor.process(image(10, 10, BufferedImage.TYPE_INT_RGB, "gif")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("El archivo no es una imagen JPG o PNG válida");
  }
}
