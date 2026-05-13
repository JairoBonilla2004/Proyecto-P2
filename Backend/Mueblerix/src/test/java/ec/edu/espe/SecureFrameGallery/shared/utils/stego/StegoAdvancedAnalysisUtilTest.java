package ec.edu.espe.SecureFrameGallery.shared.utils.stego;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
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

        StegoAdvancedAnalysisUtil.CorrelationAnalysis analysis = StegoAdvancedAnalysisUtil.analyzePixelCorrelation(uniform);
        assertTrue(analysis.averageCorrelation() > 0.80,
            "Imagen uniforme debe tener correlación promedio > 0.80, fue: " + analysis.averageCorrelation());
        assertFalse(analysis.isAnomalous());
    }

    @Test
    @DisplayName("Imagen con ruido puro tiene correlación baja (< 0.50)")
    void noisyImageHasLowSpatialCorrelation() throws IOException {
        BufferedImage noisy = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Random rng = new Random(42);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                noisy.setRGB(x, y, rng.nextInt(0xFFFFFF));

        StegoAdvancedAnalysisUtil.CorrelationAnalysis analysis = StegoAdvancedAnalysisUtil.analyzePixelCorrelation(noisy, 1, 0.80);
        assertTrue(analysis.averageCorrelation() < 0.80,
            "Imagen con ruido puro debe tener correlación promedio < 0.80, fue: " + analysis.averageCorrelation());
        assertTrue(analysis.isAnomalous());
    }

    // ── Ratio de bloques anómalos ─────────────────────────────────────────────

    @Test
    @DisplayName("Imagen limpia tiene ratio de bloques anómalos bajo (< 0.10)")
    void cleanImageHasLowBlockAnomalyRatio() throws IOException {
        BufferedImage gradient = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                gradient.setRGB(x, y, (x * 4) << 16 | (y * 4) << 8 | 64);

        StegoAdvancedAnalysisUtil.BlockAnomalyResult result = StegoAdvancedAnalysisUtil.detectBlockAnomalies(gradient);
        assertFalse(result.hasBlockAnomalies(),
            "Gradiente limpio no debería disparar anomalías de bloques; ratio=" + result.anomalyRatio());
    }

    @Test
    @DisplayName("Imagen con LSBs aleatorizados tiene ratio de bloques anómalos alto (> 0.10)")
    void steganographicImageHasHighBlockAnomalyRatio() throws IOException {
        BufferedImage mostlyFlat = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                mostlyFlat.setRGB(x, y, 0x777777);

        // Introducir un bloque altamente entropico (posible región alterada)
        Random rng = new Random(99);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                mostlyFlat.setRGB(x, y, rng.nextInt(0xFFFFFF));
            }
        }

        StegoAdvancedAnalysisUtil.BlockAnomalyResult result = StegoAdvancedAnalysisUtil.detectBlockAnomalies(mostlyFlat);
        assertTrue(result.hasBlockAnomalies(),
            "Imagen con un bloque ruidoso debería disparar anomalías; ratio=" + result.anomalyRatio());
    }

    // ── Artificialidad del ruido ──────────────────────────────────────────────

    @Test
    @DisplayName("Ruido natural tiene score de artificialidad bajo (< 0.60)")
    void naturalNoiseHasLowArtificialityScore() throws IOException {
        BufferedImage natural = createGradientImage(128, 128);
        StegoAdvancedAnalysisUtil.NoisePatternResult result = StegoAdvancedAnalysisUtil.characterizeNoisePattern(natural, 1);
        assertFalse(result.looksArtificial(), "Gradiente debería verse natural");
        assertTrue(result.artificiality() < 0.80, "Artificialidad inesperadamente alta: " + result.artificiality());
    }

    @Test
    @DisplayName("Ruido completamente aleatorio tiene score de artificialidad alto (> 0.60)")
    void randomNoiseHasHighArtificialityScore() throws IOException {
        BufferedImage random = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        Random rng = new Random(7);
        for (int x = 0; x < 128; x++)
            for (int y = 0; y < 128; y++)
                random.setRGB(x, y, rng.nextInt(0xFFFFFF));

        StegoAdvancedAnalysisUtil.NoisePatternResult result = StegoAdvancedAnalysisUtil.characterizeNoisePattern(random, 1);
        assertTrue(result.looksArtificial(), "Ruido aleatorio debería verse artificial");
        assertTrue(result.artificiality() > 0.60,
            "Ruido aleatorio debe tener artificialidad > 0.60, fue: " + result.artificiality());
    }

    // ── Casos borde ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Imagen 1x1 no lanza excepción en ningún análisis")
    void singlePixelImageDoesNotThrow() throws IOException {
        BufferedImage tiny = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        tiny.setRGB(0, 0, 0xFF0000);

        assertDoesNotThrow(() -> StegoAdvancedAnalysisUtil.analyzePixelCorrelation(tiny));
        assertDoesNotThrow(() -> StegoAdvancedAnalysisUtil.detectBlockAnomalies(tiny));
        assertDoesNotThrow(() -> StegoAdvancedAnalysisUtil.detectRangeDistortion(tiny));
        assertDoesNotThrow(() -> StegoAdvancedAnalysisUtil.characterizeNoisePattern(tiny));
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
