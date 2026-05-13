package ec.edu.espe.SecureFrameGallery.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MagicNumberUtil — detección de tipo real por bytes")
class MagicNumberUtilTest {

    // ── JPEG ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Detecta JPEG por magic bytes FF D8 FF")
    void detectsJpegMagicBytes() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        assertTrue(MagicNumberUtil.isJpeg(jpeg));
    }

    @Test
    @DisplayName("No confunde PNG con JPEG")
    void doesNotConfusePngWithJpeg() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertFalse(MagicNumberUtil.isJpeg(png));
    }

    // ── PNG ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Detecta PNG por magic bytes 89 50 4E 47")
    void detectsPngMagicBytes() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertTrue(MagicNumberUtil.isPng(png));
    }

    @Test
    @DisplayName("No detecta PNG con cabecera corrupta")
    void rejectsCorruptedPngHeader() {
        byte[] corrupted = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x00, 0x00, 0x00, 0x00};
        assertFalse(MagicNumberUtil.isPng(corrupted));
    }

    // ── GIF ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Detecta GIF87a")
    void detectsGif87a() {
        byte[] gif = new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
        assertTrue(MagicNumberUtil.isGif(gif));
    }

    @Test
    @DisplayName("Detecta GIF89a")
    void detectsGif89a() {
        byte[] gif = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
        assertTrue(MagicNumberUtil.isGif(gif));
    }

    // ── WebP ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Detecta WebP por RIFF + WEBP")
    void detectsWebP() {
        byte[] webp = new byte[]{
            0x52, 0x49, 0x46, 0x46,  // RIFF
            0x00, 0x00, 0x00, 0x00,  // size (ignored)
            0x57, 0x45, 0x42, 0x50   // WEBP
        };
        assertTrue(MagicNumberUtil.isWebP(webp));
    }

    // ── Rechazo de tipos peligrosos ──────────────────────────────────────────

    @Test
    @DisplayName("Rechaza ejecutable PE (MZ header)")
    void rejectsExecutable() {
        byte[] exe = new byte[]{0x4D, 0x5A, 0x00, 0x00};  // MZ
        assertFalse(MagicNumberUtil.isAllowedImageType(exe));
    }

    @Test
    @DisplayName("Rechaza PDF disfrazado")
    void rejectsPdf() {
        byte[] pdf = new byte[]{0x25, 0x50, 0x44, 0x46};  // %PDF
        assertFalse(MagicNumberUtil.isAllowedImageType(pdf));
    }

    @Test
    @DisplayName("Rechaza array vacío")
    void rejectsEmptyArray() {
        assertFalse(MagicNumberUtil.isAllowedImageType(new byte[]{}));
    }

    @Test
    @DisplayName("Rechaza null")
    void rejectsNull() {
        assertFalse(MagicNumberUtil.isAllowedImageType(null));
    }

    // ── Tipos permitidos ─────────────────────────────────────────────────────

    static Stream<byte[]> allowedImageHeaders() {
        return Stream.of(
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0},  // JPEG
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},  // PNG
            new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}   // GIF89a
        );
    }

    @ParameterizedTest
    @MethodSource("allowedImageHeaders")
    @DisplayName("Acepta tipos de imagen válidos")
    void acceptsAllowedImageTypes(byte[] header) {
        assertTrue(MagicNumberUtil.isAllowedImageType(header));
    }
}
