package ec.edu.espe.SecureFrameGallery.shared.utils.stego;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StegoImageTypeClassifier — clasificación de tipo de imagen para análisis")
class StegoImageTypeClassifierTest {

    @Test
    @DisplayName("Null se clasifica como PHOTOGRAPHIC por defecto")
    void nullClassifiesAsPhotographic() {
        StegoImageTypeClassifier.Classification c = StegoImageTypeClassifier.classify(null, "image/png");
        assertEquals(StegoImageTypeClassifier.ImageType.PHOTOGRAPHIC, c.type());
        assertEquals(0, c.uniqueColors());
        assertEquals(0, c.sampleCount());
    }

    @Test
    @DisplayName("JPEG siempre se trata como PHOTOGRAPHIC (sin heurísticas)")
    void jpegIsPhotographicByPolicy() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 32; x++)
            for (int y = 0; y < 32; y++)
                img.setRGB(x, y, (x ^ y) * 12345);

        StegoImageTypeClassifier.Classification c = StegoImageTypeClassifier.classify(img, "image/jpeg");
        assertEquals(StegoImageTypeClassifier.ImageType.PHOTOGRAPHIC, c.type());
        assertEquals(0, c.sampleCount());
    }

    @Test
    @DisplayName("Imagen con filas sólidas se clasifica como SYNTHETIC (PNG)")
    void solidRowsClassifyAsSynthetic() {
        BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 40; y++) {
            int rowColor = (y * 6) << 16;
            for (int x = 0; x < 40; x++) {
                img.setRGB(x, y, rowColor);
            }
        }

        StegoImageTypeClassifier.Classification c = StegoImageTypeClassifier.classify(img, "image/png");
        assertEquals(StegoImageTypeClassifier.ImageType.SYNTHETIC, c.type());
        assertTrue(c.solidRowRatio() >= 0.15);
        assertTrue(c.sampleCount() > 0);
    }

    @Test
    @DisplayName("Imagen con muchos colores se clasifica como PHOTOGRAPHIC (PNG)")
    void noisyImageClassifiesAsPhotographic() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Random rng = new Random(123);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                img.setRGB(x, y, rng.nextInt(0xFFFFFF));

        StegoImageTypeClassifier.Classification c = StegoImageTypeClassifier.classify(img, "image/png");
        assertEquals(StegoImageTypeClassifier.ImageType.PHOTOGRAPHIC, c.type());
        assertTrue(c.sampleCount() > 0);
    }
}
