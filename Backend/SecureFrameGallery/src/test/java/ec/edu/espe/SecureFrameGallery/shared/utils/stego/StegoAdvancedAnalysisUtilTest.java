package ec.edu.espe.SecureFrameGallery.shared.utils.stego;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StegoAdvancedAnalysisUtil — análisis avanzado de correlación y bloques")
class StegoAdvancedAnalysisUtilTest {

    // ── Correlación de píxeles ────────────────────────────────────────────────

    @Test
    @DisplayName("Imagen uniforme tiene correlación espacial alta (> 0.80)")
    void uniformImageHasHighSpatialCorrelation() throws IOException {
        BufferedImage uniform = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                uniform.setRGB(x, y, 0x808080);

        double correlation = StegoAdvancedAnalysisUtil.spatialCorrelation(uniform);
        assertTrue(correlation > 0.80,
            "Imagen uniforme debe tener correlación > 0.80, fue: " + correlation);
    }

    @Test
    @DisplayName("Imagen con ruido puro tiene correlación baja (< 0.50)")
    void noisyImageHasLowSpatialCorrelation() throws IOException {
        BufferedImage noisy = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Random rng = new Random(42);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                noisy.setRGB(x, y, rng.nextInt(0xFFFFFF));

        double correlation = StegoAdvancedAnalysisUtil.spatialCorrelation(noisy);
        assertTrue(correlation < 0.50,
            "Imagen con ruido puro debe tener correlación < 0.50, fue: " + correlation);
    }

    // ── Ratio de bloques anómalos ─────────────────────────────────────────────

    @Test
    @DisplayName("Imagen limpia tiene ratio de bloques anómalos bajo (< 0.10)")
    void cleanImageHasLowBlockAnomalyRatio() throws IOException {
        BufferedImage gradient = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                gradient.setRGB(x, y, (x * 4) << 16 | (y * 4) << 8 | 64);

        double ratio = StegoAdvancedAnalysisUtil.blockAnomalyRatio(gradient);
        assertTrue(ratio < 0.10,
            "Imagen de gradiente limpia debe tener ratio de anomalía < 0.10, fue: " + ratio);
    }

    @Test
    @DisplayName("Imagen con LSBs aleatorizados tiene ratio de bloques anómalos alto (> 0.10)")
    void steganographicImageHasHighBlockAnomalyRatio() throws IOException {
        BufferedImage stego = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Random rng = new Random(99);
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                // Pixel natural + LSB aleatorizado
                int base = (x * 2) << 16 | (y * 2) << 8 | 100;
                int lsbNoise = rng.nextInt(2) | (rng.nextInt(2) << 8) | (rng.nextInt(2) << 16);
                stego.setRGB(x, y, base | lsbNoise);
            }
        }

        double ratio = StegoAdvancedAnalysisUtil.blockAnomalyRatio(stego);
        assertTrue(ratio > 0.10,
            "Imagen con LSBs aleatorizados debe tener ratio > 0.10, fue: " + ratio);
    }

    // ── Artificialidad del ruido ──────────────────────────────────────────────

    @Test
    @DisplayName("Ruido natural tiene score de artificialidad bajo (< 0.60)")
    void naturalNoiseHasLowArtificialityScore() throws IOException {
        BufferedImage natural = createGradientImage(128, 128);
        double score = StegoAdvancedAnalysisUtil.noiseArtificiality(natural);
        assertTrue(score < 0.60,
            "Ruido natural debe tener score de artificialidad < 0.60, fue: " + score);
    }

    @Test
    @DisplayName("Ruido completamente aleatorio tiene score de artificialidad alto (> 0.60)")
    void randomNoiseHasHighArtificialityScore() throws IOException {
        BufferedImage random = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        Random rng = new Random(7);
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++)
                random.setRGB(x, y, rng.nextInt(0xFFFFFF));

        double score = StegoAdvancedAnalysisUtil.noiseArtificiality(random);
        assertTrue(score > 0.60,
            "Ruido completamente aleatorio debe tener score > 0.60, fue: " + score);
    }

    // ── Casos borde ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Imagen 1x1 no lanza excepción en ningún análisis")
    void singlePixelImageDoesNotThrow() throws IOException {
        BufferedImage tiny = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        tiny.setRGB(0, 0, 0xFF0000);

        assertDoesNotThrow(() -> StegoAdvancedAnalysisUtil.spatialCorrelation(tiny));
        assertDoesNotThrow(() -> StegoAdvancedAnalysisUtil.blockAnomalyRatio(tiny));
        assertDoesNotThrow(() -> StegoAdvancedAnalysisUtil.noiseArtificiality(tiny));
    }

    @Test
    @DisplayName("Null en spatialCorrelation retorna 0 sin excepción")
    void nullSpatialCorrelationReturnsZero() {
        assertDoesNotThrow(() ->
            assertEquals(0.0, StegoAdvancedAnalysisUtil.spatialCorrelation(null), 0.001)
        );
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private BufferedImage createGradientImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                img.setRGB(x, y, (x * 2) << 16 | (y * 2) << 8 | 128);
        return img;
    }
}
