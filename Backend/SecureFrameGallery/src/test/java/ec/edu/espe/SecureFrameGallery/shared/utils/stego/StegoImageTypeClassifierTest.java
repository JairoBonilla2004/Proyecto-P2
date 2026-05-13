package ec.edu.espe.SecureFrameGallery.shared.utils.stego;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StegoImageTypeClassifier — clasificación de tipo de imagen para análisis")
class StegoImageTypeClassifierTest {

    // ── Clasificación por magic bytes ─────────────────────────────────────────

    @Test
    @DisplayName("Clasifica JPEG correctamente")
    void classifiesJpegCorrectly() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        assertEquals("JPEG", StegoImageTypeClassifier.classify(jpeg));
    }

    @Test
    @DisplayName("Clasifica PNG correctamente")
    void classifiesPngCorrectly() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertEquals("PNG", StegoImageTypeClassifier.classify(png));
    }

    @Test
    @DisplayName("Clasifica GIF87a correctamente")
    void classifiesGif87aCorrectly() {
        byte[] gif = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
        assertEquals("GIF", StegoImageTypeClassifier.classify(gif));
    }

    @Test
    @DisplayName("Clasifica GIF89a correctamente")
    void classifiesGif89aCorrectly() {
        byte[] gif = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
        assertEquals("GIF", StegoImageTypeClassifier.classify(gif));
    }

    @Test
    @DisplayName("Clasifica WebP correctamente")
    void classifiesWebPCorrectly() {
        byte[] webp = {
            0x52, 0x49, 0x46, 0x46,
            0x00, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50
        };
        assertEquals("WEBP", StegoImageTypeClassifier.classify(webp));
    }

    @Test
    @DisplayName("Tipo desconocido retorna UNKNOWN")
    void unknownTypeReturnsUnknown() {
        byte[] unknown = {0x00, 0x01, 0x02, 0x03};
        assertEquals("UNKNOWN", StegoImageTypeClassifier.classify(unknown));
    }

    @Test
    @DisplayName("Null retorna UNKNOWN sin excepción")
    void nullReturnsUnknown() {
        assertDoesNotThrow(() -> assertEquals("UNKNOWN", StegoImageTypeClassifier.classify(null)));
    }

    @Test
    @DisplayName("Array vacío retorna UNKNOWN sin excepción")
    void emptyArrayReturnsUnknown() {
        assertDoesNotThrow(() -> assertEquals("UNKNOWN", StegoImageTypeClassifier.classify(new byte[]{})));
    }

    // ── Análisis aplicable por tipo ───────────────────────────────────────────

    @Test
    @DisplayName("JPEG es candidato para análisis LSB")
    void jpegIsCandidateForLsbAnalysis() {
        assertTrue(StegoImageTypeClassifier.supportsLsbAnalysis("JPEG"));
    }

    @Test
    @DisplayName("PNG es candidato para análisis LSB")
    void pngIsCandidateForLsbAnalysis() {
        assertTrue(StegoImageTypeClassifier.supportsLsbAnalysis("PNG"));
    }

    @Test
    @DisplayName("GIF NO es candidato para análisis LSB estándar")
    void gifIsNotCandidateForLsbAnalysis() {
        assertFalse(StegoImageTypeClassifier.supportsLsbAnalysis("GIF"));
    }

    @Test
    @DisplayName("UNKNOWN NO es candidato para análisis LSB")
    void unknownIsNotCandidateForLsbAnalysis() {
        assertFalse(StegoImageTypeClassifier.supportsLsbAnalysis("UNKNOWN"));
    }

    // ── Análisis EOF aplicable ────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({"JPEG,true", "PNG,true", "GIF,true", "WEBP,false", "UNKNOWN,false"})
    @DisplayName("Verifica qué tipos soportan análisis EOF")
    void eofAnalysisSupportByType(String type, boolean expected) {
        assertEquals(expected, StegoImageTypeClassifier.supportsEofAnalysis(type));
    }
}
