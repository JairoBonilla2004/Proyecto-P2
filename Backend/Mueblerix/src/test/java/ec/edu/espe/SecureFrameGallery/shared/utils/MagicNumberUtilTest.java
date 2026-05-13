package ec.edu.espe.SecureFrameGallery.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MagicNumberUtil — detección de tipo real por bytes")
class MagicNumberUtilTest {

    private final MagicNumberUtil util = new MagicNumberUtil();

    // ── JPEG ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Detecta JPEG por magic bytes FF D8 FF")
    void detectsJpegMagicBytes() {
        byte[] jpeg = padTo12(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
        assertEquals("image/jpeg", util.detectAndValidate(jpeg));
    }

    @Test
    @DisplayName("No confunde PNG con JPEG")
    void doesNotConfusePngWithJpeg() {
        byte[] png = padTo12(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        assertNotEquals("image/jpeg", util.detectAndValidate(png));
    }

    // ── PNG ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Detecta PNG por magic bytes 89 50 4E 47")
    void detectsPngMagicBytes() {
        byte[] png = padTo12(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        assertEquals("image/png", util.detectAndValidate(png));
    }

    @Test
    @DisplayName("No detecta PNG con cabecera corrupta")
    void rejectsCorruptedPngHeader() {
        byte[] corrupted = padTo12(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x00, 0x00, 0x00, 0x00});
        assertThrows(IllegalArgumentException.class, () -> util.detectAndValidate(corrupted));
    }

    // ── GIF ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Detecta GIF87a")
    void detectsGif87a() {
        byte[] gif = padTo12(new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61});
        assertEquals("image/gif", util.detectAndValidate(gif));
    }

    @Test
    @DisplayName("Detecta GIF89a")
    void detectsGif89a() {
        byte[] gif = padTo12(new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61});
        assertEquals("image/gif", util.detectAndValidate(gif));
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
        assertEquals("image/webp", util.detectAndValidate(webp));
    }

    // ── Rechazo de tipos peligrosos ──────────────────────────────────────────

    @Test
    @DisplayName("Rechaza ejecutable PE (MZ header)")
    void rejectsExecutable() {
        byte[] exe = padTo12(new byte[]{0x4D, 0x5A, 0x00, 0x00});  // MZ
        assertThrows(IllegalArgumentException.class, () -> util.detectAndValidate(exe));
    }

    @Test
    @DisplayName("Rechaza PDF disfrazado")
    void rejectsPdf() {
        byte[] pdf = padTo12(new byte[]{0x25, 0x50, 0x44, 0x46});  // %PDF
        assertThrows(IllegalArgumentException.class, () -> util.detectAndValidate(pdf));
    }

    @Test
    @DisplayName("Rechaza array vacío")
    void rejectsEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> util.detectAndValidate(new byte[]{}));
    }

    @Test
    @DisplayName("Rechaza null")
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> util.detectAndValidate(null));
    }

    private static byte[] padTo12(byte[] header) {
        byte[] padded = new byte[12];
        System.arraycopy(header, 0, padded, 0, Math.min(header.length, 12));
        return padded;
    }
}
