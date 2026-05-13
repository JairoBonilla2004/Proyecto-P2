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
        assertFalse(StegoEofUtil.detectEofAnomaly(cleanJpeg, "image/jpeg"));
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
        assertTrue(StegoEofUtil.detectEofAnomaly(jpegWithPayload, "image/jpeg"));
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
        assertFalse(StegoEofUtil.detectEofAnomaly(cleanPng, "image/png"));
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
        assertTrue(StegoEofUtil.detectEofAnomaly(pngWithPayload, "image/png"));
    }

    // ── Casos borde ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Array vacío no lanza excepción y retorna false")
    void emptyArrayReturnsFalse() {
        assertDoesNotThrow(() -> assertFalse(StegoEofUtil.detectEofAnomaly(new byte[]{}, "image/jpeg")));
    }

    @Test
    @DisplayName("Null no lanza excepción y retorna false")
    void nullReturnsFalse() {
        assertDoesNotThrow(() -> assertFalse(StegoEofUtil.detectEofAnomaly(null, "image/jpeg")));
    }

    @Test
    @DisplayName("Archivo que no es JPEG ni PNG retorna false")
    void unknownFormatReturnsFalse() {
        byte[] unknown = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04};
        assertFalse(StegoEofUtil.detectEofAnomaly(unknown, "application/octet-stream"));
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
        assertFalse(StegoEofUtil.detectEofAnomaly(jpeg, "image/jpeg"));
    }

    @Test
    @DisplayName("Cualquier byte extra tras EOF se considera anomalía")
    void trailingBytesTriggerAnomaly() {
        byte[] jpeg = new byte[]{
            (byte) 0xFF, (byte) 0xD8,
            (byte) 0xFF, (byte) 0xD9,
            0x01, 0x02, 0x03, 0x04
        };
        assertTrue(StegoEofUtil.detectEofAnomaly(jpeg, "image/jpeg"));
    }
}
