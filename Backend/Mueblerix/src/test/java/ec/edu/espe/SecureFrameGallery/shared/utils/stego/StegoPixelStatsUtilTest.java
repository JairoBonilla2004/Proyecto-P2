package ec.edu.espe.SecureFrameGallery.shared.utils.stego;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StegoPixelStatsUtil — estadísticas de píxeles para detección LSB")
class StegoPixelStatsUtilTest {

    @Test
    @DisplayName("pickStride selecciona stride según tamaño")
    void pickStrideSelectsExpectedValues() {
        assertEquals(1, StegoPixelStatsUtil.pickStride(1000, 1000)); // 1,000,000 px
        assertEquals(2, StegoPixelStatsUtil.pickStride(2000, 1200)); // 2,400,000 px
        assertEquals(3, StegoPixelStatsUtil.pickStride(4000, 2500)); // 10,000,000 px
    }

    @Test
    @DisplayName("computeSmoothRatio en imagen uniforme es alto")
    void smoothRatioIsHighOnUniformImage() {
        BufferedImage uniform = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                uniform.setRGB(x, y, 0x777777);

        double ratio = StegoPixelStatsUtil.computeSmoothRatio(uniform, 0);
        assertTrue(ratio > 0.95, "Smooth ratio esperado alto, fue: " + ratio);
    }

    @Test
    @DisplayName("computeSmoothRatio en checkerboard es bajo")
    void smoothRatioIsLowOnCheckerboard() {
        BufferedImage checker = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                checker.setRGB(x, y, ((x + y) % 2 == 0) ? 0x000000 : 0xFFFFFF);

        double ratio = StegoPixelStatsUtil.computeSmoothRatio(checker, 0);
        assertTrue(ratio < 0.10, "Smooth ratio esperado bajo, fue: " + ratio);
    }

    @Test
    @DisplayName("measureLsbTransitionRate es ~0 en imagen uniforme")
    void transitionRateIsLowOnUniformImage() {
        BufferedImage uniform = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 32; x++)
            for (int y = 0; y < 32; y++)
                uniform.setRGB(x, y, 0x222222);

        StegoPixelStatsUtil.TransitionRate r = StegoPixelStatsUtil.measureLsbTransitionRate(uniform, 0, 32, false, 0);
        assertTrue(r.samples() > 0);
        assertTrue(r.rate() < 0.05, "Tasa de transición esperada baja, fue: " + r.rate());
    }

    @Test
    @DisplayName("measureLsbTransitionRate es alta en patrón alternante")
    void transitionRateIsHighOnAlternatingPattern() {
        BufferedImage alt = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                int v = ((x + y) % 2 == 0) ? 0x000000 : 0x010101; // LSB alternante
                alt.setRGB(x, y, v);
            }
        }

        StegoPixelStatsUtil.TransitionRate r = StegoPixelStatsUtil.measureLsbTransitionRate(alt, 0, 64, false, 0);
        assertTrue(r.samples() > 0);
        assertTrue(r.rate() > 0.30, "Tasa de transición esperada alta, fue: " + r.rate());
    }
}
