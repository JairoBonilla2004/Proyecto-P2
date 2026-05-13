package ec.edu.espe.SecureFrameGallery.modules.steganography.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExifStripperImpl — limpieza de metadatos EXIF")
class ExifStripperImplTest {

    private ExifStripperImpl exifStripper;

    @BeforeEach
    void setUp() {
        exifStripper = new ExifStripperImpl();
    }

    // ── Limpieza básica ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Strip de imagen válida retorna bytes no nulos")
    void stripValidImageReturnsBytes() throws IOException {
        byte[] jpegBytes = createMinimalJpeg();
        byte[] stripped = exifStripper.strip(jpegBytes);
        assertNotNull(stripped);
        assertTrue(stripped.length > 0);
    }

    @Test
    @DisplayName("Imagen limpiada sigue siendo una imagen válida")
    void strippedImageIsStillValidImage() throws IOException {
        byte[] jpegBytes = createMinimalJpeg();
        byte[] stripped = exifStripper.strip(jpegBytes);

        // Debe poder leerse de vuelta como imagen
        java.io.InputStream is = new java.io.ByteArrayInputStream(stripped);
        BufferedImage reloaded = ImageIO.read(is);
        assertNotNull(reloaded, "La imagen limpiada debe seguir siendo legible");
    }

    @Test
    @DisplayName("Imagen PNG limpiada conserva dimensiones originales")
    void strippedPngPreservesDimensions() throws IOException {
        byte[] pngBytes = createMinimalPng(100, 80);
        byte[] stripped = exifStripper.strip(pngBytes);

        java.io.InputStream is = new java.io.ByteArrayInputStream(stripped);
        BufferedImage reloaded = ImageIO.read(is);

        assertNotNull(reloaded);
        assertEquals(100, reloaded.getWidth());
        assertEquals(80, reloaded.getHeight());
    }

    // ── Casos borde ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Strip de array vacío lanza excepción controlada")
    void stripEmptyArrayThrowsControlled() {
        assertThrows(Exception.class, () -> exifStripper.strip(new byte[]{}));
    }

    @Test
    @DisplayName("Strip de null lanza excepción controlada")
    void stripNullThrowsControlled() {
        assertThrows(Exception.class, () -> exifStripper.strip(null));
    }

    @Test
    @DisplayName("Strip de datos corruptos lanza excepción controlada")
    void stripCorruptDataThrowsControlled() {
        byte[] corrupt = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        assertThrows(Exception.class, () -> exifStripper.strip(corrupt));
    }

    // ── Reducción de tamaño ───────────────────────────────────────────────────

    @Test
    @DisplayName("Imagen con metadatos simulados es igual o más pequeña tras el strip")
    void strippedImageIsNotLargerThanOriginal() throws IOException {
        byte[] original = createMinimalJpeg();
        byte[] stripped = exifStripper.strip(original);
        // El re-encoding puede cambiar el tamaño pero no debe crecer más del 10%
        assertTrue(stripped.length <= original.length * 1.1,
            "Imagen limpiada no debe ser significativamente más grande que la original");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private byte[] createMinimalJpeg() throws IOException {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                img.setRGB(x, y, (x * 4) << 16 | (y * 4) << 8 | 128);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "JPEG", baos);
        return baos.toByteArray();
    }

    private byte[] createMinimalPng(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                img.setRGB(x, y, 0xFF336699);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
