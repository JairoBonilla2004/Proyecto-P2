package ec.edu.espe.SecureFrameGallery.shared.utils.stego;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StegoEofUtil — detección de datos tras marcador EOF")
class StegoEofUtilTest {

    // ── JPEG limpio ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("JPEG sin datos extra tras FFD9 no tiene anomalía EOF")
    void cleanJpegHasNoEofAnomaly() {
        // JPEG mínimo: SOI + EOI
        byte[] cleanJpeg = new byte[]{
            (byte) 0xFF, (byte) 0xD8,  // SOI
            (byte) 0xFF, (byte) 0xD9   // EOI
        };
        assertFalse(StegoEofUtil.hasEofAnomaly(cleanJpeg));
    }

    @Test
    @DisplayName("JPEG con datos extra tras FFD9 tiene anomalía EOF")
    void jpegWithTrailingDataHasEofAnomaly() {
        byte[] jpegWithPayload = new byte[]{
            (byte) 0xFF, (byte) 0xD8,  // SOI
            (byte) 0xFF, (byte) 0xD9,  // EOI
            0x53, 0x65, 0x63, 0x72,    // datos ocultos: "Secr"
            0x65, 0x74, 0x44, 0x61     // "etDa"
        };
        assertTrue(StegoEofUtil.hasEofAnomaly(jpegWithPayload));
    }

    // ── Umbral de bytes extra ─────────────────────────────────────────────────

    @Test
    @DisplayName("Pocos bytes tras EOF (ruido de encoder) no activa alerta")
    void fewTrailingBytesDoNotTriggerAlert() {
        // Algunos encoders agregan 1-2 bytes de padding — no debe ser falso positivo
        byte[] jpegWithPadding = new byte[]{
            (byte) 0xFF, (byte) 0xD8,
            (byte) 0xFF, (byte) 0xD9,
            0x00  // 1 byte de padding
        };
        assertFalse(StegoEofUtil.hasEofAnomaly(jpegWithPadding));
    }

    @Test
    @DisplayName("Más de 4 bytes tras EOF activa alerta")
    void manyTrailingBytesTriggersAlert() {
        byte[] suspicious = new byte[]{
            (byte) 0xFF, (byte) 0xD8,
            (byte) 0xFF, (byte) 0xD9,
            0x01, 0x02, 0x03, 0x04, 0x05  // 5 bytes extra
        };
        assertTrue(StegoEofUtil.hasEofAnomaly(suspicious));
    }

    // ── PNG ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PNG limpio (termina en IEND chunk) no tiene anomalía")
    void cleanPngHasNoEofAnomaly() {
        // PNG mínimo: signature + IEND chunk (AE 42 60 82 es el CRC de IEND)
        byte[] cleanPng = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,  // PNG signature
            0x00, 0x00, 0x00, 0x00,  // IEND length = 0
            0x49, 0x45, 0x4E, 0x44, // "IEND"
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82  // CRC
        };
        assertFalse(StegoEofUtil.hasEofAnomaly(cleanPng));
    }

    @Test
    @DisplayName("PNG con datos tras IEND tiene anomalía EOF")
    void pngWithDataAfterIendHasAnomaly() {
        byte[] pngWithPayload = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x00,
            0x49, 0x45, 0x4E, 0x44,
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82,
            // payload oculto
            0x68, 0x69, 0x64, 0x64, 0x65, 0x6E  // "hidden"
        };
        assertTrue(StegoEofUtil.hasEofAnomaly(pngWithPayload));
    }

    // ── Casos borde ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Array vacío no lanza excepción y retorna false")
    void emptyArrayReturnsFalse() {
        assertDoesNotThrow(() -> assertFalse(StegoEofUtil.hasEofAnomaly(new byte[]{})));
    }

    @Test
    @DisplayName("Null no lanza excepción y retorna false")
    void nullReturnsFalse() {
        assertDoesNotThrow(() -> assertFalse(StegoEofUtil.hasEofAnomaly(null)));
    }

    @Test
    @DisplayName("Archivo que no es JPEG ni PNG retorna false")
    void unknownFormatReturnsFalse() {
        byte[] unknown = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04};
        assertFalse(StegoEofUtil.hasEofAnomaly(unknown));
    }

    // ── Exactitud del marcador ────────────────────────────────────────────────

    @Test
    @DisplayName("JPEG con FFD9 en posición intermedia no activa falso positivo")
    void jpegWithMidstreamEoiDoesNotFalsePositive() {
        // FFD9 aparece en medio del stream pero el archivo termina limpio
        byte[] jpeg = new byte[]{
            (byte) 0xFF, (byte) 0xD8,
            (byte) 0xFF, (byte) 0xD9,  // EOI falso en medio
            0x10, 0x20, 0x30,
            (byte) 0xFF, (byte) 0xD9   // EOI real al final
        };
        assertFalse(StegoEofUtil.hasEofAnomaly(jpeg));
    }

    @Test
    @DisplayName("Cantidad exacta de 4 bytes extra no activa alerta (límite inferior)")
    void exactlyFourTrailingBytesDoNotTrigger() {
        byte[] jpeg = new byte[]{
            (byte) 0xFF, (byte) 0xD8,
            (byte) 0xFF, (byte) 0xD9,
            0x01, 0x02, 0x03, 0x04
        };
        assertFalse(StegoEofUtil.hasEofAnomaly(jpeg));
    }
}
