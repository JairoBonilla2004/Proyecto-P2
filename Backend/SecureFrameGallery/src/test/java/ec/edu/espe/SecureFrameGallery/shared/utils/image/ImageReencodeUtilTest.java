package ec.edu.espe.SecureFrameGallery.shared.utils.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageReencodeUtil — re-codificación defensiva de imágenes")
class ImageReencodeUtilTest {

    // ── Re-encoding básico ────────────────────────────────────────────────────

    @Test
    @DisplayName("Re-encode de JPEG retorna bytes no nulos")
    void reencodeJpegReturnsBytes() throws IOException {
        byte[] jpeg = createJpeg(64, 64);
        byte[] result = ImageReencodeUtil.reencode(jpeg, "JPEG", 0.92f);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Re-encode de PNG retorna bytes no nulos")
    void reencodePngReturnsBytes() throws IOException {
        byte[] png = createPng(64, 64);
        byte[] result = ImageReencodeUtil.reencode(png, "PNG", 1.0f);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Imagen re-codificada sigue siendo legible")
    void reencodedImageIsReadable() throws IOException {
        byte[] jpeg = createJpeg(128, 128);
        byte[] reencoded = ImageReencodeUtil.reencode(jpeg, "JPEG", 0.92f);

        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(reencoded));
        assertNotNull(img, "La imagen re-codificada debe poder leerse de vuelta");
    }

    @Test
    @DisplayName("Imagen re-codificada conserva dimensiones originales")
    void reencodedImagePreservesDimensions() throws IOException {
        byte[] png = createPng(100, 75);
        byte[] reencoded = ImageReencodeUtil.reencode(png, "PNG", 1.0f);

        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(reencoded));
        assertNotNull(img);
        assertEquals(100, img.getWidth());
        assertEquals(75, img.getHeight());
    }

    // ── Destrucción de payload oculto ─────────────────────────────────────────

    @Test
    @DisplayName("Re-encode elimina datos extra tras marcador EOF")
    void reencodeRemovesTrailingEofData() throws IOException {
        byte[] jpeg = createJpeg(64, 64);

        // Simular payload oculto tras EOF
        byte[] withPayload = new byte[jpeg.length + 16];
        System.arraycopy(jpeg, 0, withPayload, 0, jpeg.length);
        withPayload[jpeg.length]     = 0x53; // "S"
        withPayload[jpeg.length + 1] = 0x45; // "E"
        withPayload[jpeg.length + 2] = 0x43; // "C"
        withPayload[jpeg.length + 3] = 0x52; // "R"
        for (int i = 4; i < 16; i++) withPayload[jpeg.length + i] = (byte) i;

        byte[] reencoded = ImageReencodeUtil.reencode(withPayload, "JPEG", 0.92f);

        // El re-encoding debe producir un JPEG limpio sin el payload
        assertTrue(reencoded.length < withPayload.length,
            "Re-encode debe eliminar datos extra del payload");
    }

    @Test
    @DisplayName("Re-encode con calidad 0.92 produce archivo de tamaño razonable")
    void reencodeWithQuality092ProducesReasonableSize() throws IOException {
        byte[] jpeg = createJpeg(256, 256);
        byte[] reencoded = ImageReencodeUtil.reencode(jpeg, "JPEG", 0.92f);

        // No debe ser ni vacío ni desproporcionadamente grande
        assertTrue(reencoded.length > 100, "Archivo re-codificado demasiado pequeño");
        assertTrue(reencoded.length < jpeg.length * 3, "Archivo re-codificado demasiado grande");
    }

    // ── Formato de salida ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Re-encode a JPEG produce magic bytes FF D8 FF")
    void reencodeToJpegProducesCorrectMagicBytes() throws IOException {
        byte[] png = createPng(64, 64);
        byte[] reencoded = ImageReencodeUtil.reencode(png, "JPEG", 0.92f);

        assertEquals((byte) 0xFF, reencoded[0]);
        assertEquals((byte) 0xD8, reencoded[1]);
        assertEquals((byte) 0xFF, reencoded[2]);
    }

    @Test
    @DisplayName("Re-encode a PNG produce magic bytes 89 50 4E 47")
    void reencodeToPngProducesCorrectMagicBytes() throws IOException {
        byte[] jpeg = createJpeg(64, 64);
        byte[] reencoded = ImageReencodeUtil.reencode(jpeg, "PNG", 1.0f);

        assertEquals((byte) 0x89, reencoded[0]);
        assertEquals((byte) 0x50, reencoded[1]);
        assertEquals((byte) 0x4E, reencoded[2]);
        assertEquals((byte) 0x47, reencoded[3]);
    }

    // ── Casos borde ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Re-encode de null lanza excepción controlada")
    void reencodeNullThrows() {
        assertThrows(Exception.class, () -> ImageReencodeUtil.reencode(null, "JPEG", 0.92f));
    }

    @Test
    @DisplayName("Re-encode de datos corruptos lanza excepción controlada")
    void reencodeCorruptDataThrows() {
        byte[] corrupt = {0x00, 0x01, 0x02, 0x03};
        assertThrows(Exception.class, () -> ImageReencodeUtil.reencode(corrupt, "JPEG", 0.92f));
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
