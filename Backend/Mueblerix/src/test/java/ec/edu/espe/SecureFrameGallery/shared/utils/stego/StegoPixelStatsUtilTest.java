package ec.edu.espe.SecureFrameGallery.shared.utils.stego;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StegoPixelStatsUtil — estadísticas de píxeles para detección LSB")
class StegoPixelStatsUtilTest {

    // ── Entropía de Shannon ───────────────────────────────────────────────────

    @Test
    @DisplayName("Entropía de distribución uniforme es máxima (~1.0)")
    void uniformDistributionHasMaxEntropy() {
        // Todos los valores de LSB distribuidos uniformemente
        int[] uniformLsb = new int[256];
        for (int i = 0; i < 256; i++) uniformLsb[i] = 100;

        double entropy = StegoPixelStatsUtil.shannonEntropy(uniformLsb);
        assertTrue(entropy > 0.99, "Distribución uniforme debe tener entropía cercana a 1.0, fue: " + entropy);
    }

    @Test
    @DisplayName("Entropía de distribución constante es 0")
    void constantDistributionHasZeroEntropy() {
        // Solo un valor presente
        int[] constantLsb = new int[256];
        constantLsb[0] = 1000;

        double entropy = StegoPixelStatsUtil.shannonEntropy(constantLsb);
        assertEquals(0.0, entropy, 0.001);
    }

    @Test
    @DisplayName("Entropía está siempre entre 0 y 1")
    void entropyIsBetweenZeroAndOne() {
        int[] randomish = new int[]{500, 200, 300, 0, 100, 400, 150, 250};
        double entropy = StegoPixelStatsUtil.shannonEntropy(randomish);
        assertTrue(entropy >= 0.0 && entropy <= 1.0,
            "Entropía fuera de rango [0,1]: " + entropy);
    }

    // ── Chi-square ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Chi-square de distribución uniforme retorna p-value alto")
    void chiSquareOfUniformDistributionReturnsHighPValue() {
        int[] uniform = new int[256];
        for (int i = 0; i < 256; i++) uniform[i] = 100;

        double pValue = StegoPixelStatsUtil.chiSquarePValue(uniform);
        assertTrue(pValue > 0.80,
            "Distribución uniforme (típica en esteganografía) debe tener p-value alto, fue: " + pValue);
    }

    @Test
    @DisplayName("Chi-square de imagen natural retorna p-value bajo")
    void chiSquareOfNaturalImageReturnsLowPValue() {
        // Distribución bimodal — típica de imagen natural
        int[] natural = new int[256];
        natural[10] = 800;
        natural[200] = 600;
        natural[128] = 100;

        double pValue = StegoPixelStatsUtil.chiSquarePValue(natural);
        assertTrue(pValue < 0.50,
            "Distribución bimodal (imagen natural) debe tener p-value bajo, fue: " + pValue);
    }

    // ── Transiciones secuenciales ─────────────────────────────────────────────

    @Test
    @DisplayName("Alta tasa de transición 0→1 y 1→0 indica LSB aleatorizado")
    void highTransitionRateIndicatesRandomizedLsb() {
        // Alternancia perfecta 0,1,0,1,... = esteganografía típica
        int[] alternating = new int[1000];
        for (int i = 0; i < 1000; i++) alternating[i] = i % 2;

        double transitionRate = StegoPixelStatsUtil.sequentialTransitionRate(alternating);
        assertTrue(transitionRate > 0.90,
            "Alternancia perfecta debe dar tasa de transición > 0.90, fue: " + transitionRate);
    }

    @Test
    @DisplayName("Baja tasa de transición indica imagen natural (LSBs correlacionados)")
    void lowTransitionRateIndicatesNaturalImage() {
        // Bloques largos del mismo bit — típico de imagen natural uniforme
        int[] blocky = new int[1000];
        for (int i = 0; i < 500; i++) blocky[i] = 0;
        for (int i = 500; i < 1000; i++) blocky[i] = 1;

        double transitionRate = StegoPixelStatsUtil.sequentialTransitionRate(blocky);
        assertTrue(transitionRate < 0.10,
            "Bloques largos deben dar tasa de transición < 0.10, fue: " + transitionRate);
    }

    // ── Casos borde ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Array vacío en shannonEntropy retorna 0 sin excepción")
    void emptyArrayShannonEntropyReturnsZero() {
        assertDoesNotThrow(() -> assertEquals(0.0, StegoPixelStatsUtil.shannonEntropy(new int[]{})));
    }

    @Test
    @DisplayName("Null en chiSquarePValue retorna 0 sin excepción")
    void nullChiSquarePValueReturnsZero() {
        assertDoesNotThrow(() -> assertEquals(0.0, StegoPixelStatsUtil.chiSquarePValue(null), 0.001));
    }

    @Test
    @DisplayName("Array de un elemento en transitionRate retorna 0")
    void singleElementTransitionRateReturnsZero() {
        assertDoesNotThrow(() ->
            assertEquals(0.0, StegoPixelStatsUtil.sequentialTransitionRate(new int[]{1}), 0.001)
        );
    }
}
