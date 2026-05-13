package ec.edu.espe.SecureFrameGallery.shared.utils.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageReencodeUtil — re-codificación defensiva de imágenes")
class ImageReencodeUtilTest {

    // ── Re-encoding básico ────────────────────────────────────────────────────

    @Test
    @DisplayName("Re-encode de JPEG retorna bytes no nulos")
    void reencodeJpegReturnsBytes() throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(createJpeg(64, 64)));
        byte[] result = ImageReencodeUtil.reencode(img, "jpeg", 0.92f);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Re-encode de PNG retorna bytes no nulos")
    void reencodePngReturnsBytes() throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(createPng(64, 64)));
        byte[] result = ImageReencodeUtil.reencode(img, "png", 1.0f);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Imagen re-codificada sigue siendo legible")
    void reencodedImageIsReadable() throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(createJpeg(128, 128)));
        byte[] reencoded = ImageReencodeUtil.reencode(img, "jpeg", 0.92f);

        BufferedImage reloaded = ImageIO.read(new ByteArrayInputStream(reencoded));
        assertNotNull(reloaded, "La imagen re-codificada debe poder leerse de vuelta");
    }

    @Test
    @DisplayName("Imagen re-codificada conserva dimensiones originales")
    void reencodedImagePreservesDimensions() throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(createPng(100, 75)));
        byte[] reencoded = ImageReencodeUtil.reencode(img, "png", 1.0f);

        BufferedImage reloaded = ImageIO.read(new ByteArrayInputStream(reencoded));
        assertNotNull(reloaded);
        assertEquals(100, reloaded.getWidth());
        assertEquals(75, reloaded.getHeight());
    }

    @Test
    @DisplayName("Re-encode con calidad 0.92 produce archivo de tamaño razonable")
    void reencodeWithQuality092ProducesReasonableSize() throws IOException {
        byte[] jpegBytes = createJpeg(256, 256);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpegBytes));
        byte[] reencoded = ImageReencodeUtil.reencode(img, "jpeg", 0.92f);

        // No debe ser ni vacío ni desproporcionadamente grande
        assertTrue(reencoded.length > 100, "Archivo re-codificado demasiado pequeño");
        assertTrue(reencoded.length < jpegBytes.length * 3, "Archivo re-codificado demasiado grande");
    }

    // ── Formato de salida ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Re-encode a JPEG produce magic bytes FF D8 FF")
    void reencodeToJpegProducesCorrectMagicBytes() throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(createPng(64, 64)));
        byte[] reencoded = ImageReencodeUtil.reencode(img, "jpeg", 0.92f);

        assertEquals((byte) 0xFF, reencoded[0]);
        assertEquals((byte) 0xD8, reencoded[1]);
        assertEquals((byte) 0xFF, reencoded[2]);
    }

    @Test
    @DisplayName("Re-encode a PNG produce magic bytes 89 50 4E 47")
    void reencodeToPngProducesCorrectMagicBytes() throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(createJpeg(64, 64)));
        byte[] reencoded = ImageReencodeUtil.reencode(img, "png", 1.0f);

        assertEquals((byte) 0x89, reencoded[0]);
        assertEquals((byte) 0x50, reencoded[1]);
        assertEquals((byte) 0x4E, reencoded[2]);
        assertEquals((byte) 0x47, reencoded[3]);
    }

    // ── Casos borde ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Re-encode de null lanza excepción controlada")
    void reencodeNullThrows() {
        assertThrows(Exception.class, () -> ImageReencodeUtil.reencode(null, "jpeg", 0.92f));
    }

    @Test
    @DisplayName("Re-encode con formato no soportado lanza IOException")
    void reencodeUnsupportedFormatThrows() throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(createPng(32, 32)));
        assertThrows(IOException.class, () -> ImageReencodeUtil.reencode(img, "bogus", 1.0f));
    }

    @Test
    @DisplayName("resolveOutputFormat mapea MIME types a formatos de salida")
    void resolveOutputFormatMapsMimeTypes() {
        assertEquals("jpeg", ImageReencodeUtil.resolveOutputFormat("image/jpeg"));
        assertEquals("png", ImageReencodeUtil.resolveOutputFormat("image/png"));
        assertEquals("png", ImageReencodeUtil.resolveOutputFormat("image/webp"));
        assertEquals("png", ImageReencodeUtil.resolveOutputFormat("image/gif"));
        assertEquals("png", ImageReencodeUtil.resolveOutputFormat("unknown/unknown"));
        assertEquals("png", ImageReencodeUtil.resolveOutputFormat(null));
    }

    @Test
    @DisplayName("clampJpegQuality limita valores fuera de rango")
    void clampJpegQualityClamps() {
        assertEquals(0.0f, ImageReencodeUtil.clampJpegQuality(-1.0f), 0.0001);
        assertEquals(1.0f, ImageReencodeUtil.clampJpegQuality(2.0f), 0.0001);
        assertEquals(0.42f, ImageReencodeUtil.clampJpegQuality(0.42f), 0.0001);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private byte[] createJpeg(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random rng = new Random(1);
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                img.setRGB(x, y, (x * 2) << 16 | (y * 2) << 8 | rng.nextInt(256));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "JPEG", baos);
        return baos.toByteArray();
    }

    private byte[] createPng(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                img.setRGB(x, y, 0xFF000000 | (x * 2) << 16 | (y * 2) << 8 | 100);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
