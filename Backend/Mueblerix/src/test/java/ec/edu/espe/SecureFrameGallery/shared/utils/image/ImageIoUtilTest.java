package ec.edu.espe.SecureFrameGallery.shared.utils.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageIoUtil — lectura y escritura segura de imágenes")
class ImageIoUtilTest {

    // ── Lectura ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Lee JPEG válido y retorna BufferedImage no nulo")
    void readsValidJpegSuccessfully() throws IOException {
        byte[] jpeg = createJpeg(64, 64);
        BufferedImage img = ImageIoUtil.readSafely(jpeg);
        assertNotNull(img);
    }

    @Test
    @DisplayName("Lee PNG válido y retorna BufferedImage no nulo")
    void readsValidPngSuccessfully() throws IOException {
        byte[] png = createPng(64, 64);
        BufferedImage img = ImageIoUtil.readSafely(png);
        assertNotNull(img);
    }

    @Test
    @DisplayName("Lee imagen y conserva dimensiones correctas")
    void readsImageWithCorrectDimensions() throws IOException {
        byte[] jpeg = createJpeg(120, 80);
        BufferedImage img = ImageIoUtil.readSafely(jpeg);
        assertNotNull(img);
        assertEquals(120, img.getWidth());
        assertEquals(80, img.getHeight());
    }

    @Test
    @DisplayName("Leer datos corruptos lanza excepción controlada")
    void readCorruptDataThrowsException() {
        byte[] corrupt = {0x00, 0x01, 0x02, 0x03, 0x04};
        assertThrows(Exception.class, () -> ImageIoUtil.readSafely(corrupt));
    }

    @Test
    @DisplayName("Leer null lanza excepción controlada")
    void readNullThrowsException() {
        assertThrows(Exception.class, () -> ImageIoUtil.readSafely(null));
    }

    @Test
    @DisplayName("Leer array vacío lanza excepción controlada")
    void readEmptyArrayThrowsException() {
        assertThrows(Exception.class, () -> ImageIoUtil.readSafely(new byte[]{}));
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Escribe BufferedImage a bytes JPEG correctamente")
    void writesBufferedImageToJpegBytes() throws IOException {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        img.setRGB(32, 32, 0xFF0000);

        byte[] result = ImageIoUtil.writeToBytes(img, "JPEG", 0.9f);

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertEquals((byte) 0xFF, result[0]);
        assertEquals((byte) 0xD8, result[1]);
    }

    @Test
    @DisplayName("Escribe BufferedImage a bytes PNG correctamente")
    void writesBufferedImageToPngBytes() throws IOException {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(32, 32, 0xFF336699);

        byte[] result = ImageIoUtil.writeToBytes(img, "PNG", 1.0f);

        assertNotNull(result);
        assertEquals((byte) 0x89, result[0]);
        assertEquals((byte) 0x50, result[1]);
    }

    @Test
    @DisplayName("Escribir null lanza excepción controlada")
    void writeNullImageThrowsException() {
        assertThrows(Exception.class, () -> ImageIoUtil.writeToBytes(null, "JPEG", 0.9f));
    }

    // ── Roundtrip ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Roundtrip read→write conserva dimensiones")
    void roundtripPreservesDimensions() throws IOException {
        byte[] original = createPng(90, 60);

        BufferedImage img = ImageIoUtil.readSafely(original);
        byte[] written = ImageIoUtil.writeToBytes(img, "PNG", 1.0f);
        BufferedImage reloaded = ImageIoUtil.readSafely(written);

        assertEquals(90, reloaded.getWidth());
        assertEquals(60, reloaded.getHeight());
    }

    @Test
    @DisplayName("Roundtrip JPEG→PNG produce PNG válido")
    void roundtripJpegToPngProducesValidPng() throws IOException {
        byte[] jpeg = createJpeg(64, 64);

        BufferedImage img = ImageIoUtil.readSafely(jpeg);
        byte[] png = ImageIoUtil.writeToBytes(img, "PNG", 1.0f);

        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 0x50, png[1]);
        assertEquals((byte) 0x4E, png[2]);
        assertEquals((byte) 0x47, png[3]);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private byte[] createJpeg(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                img.setRGB(x, y, (x * 3) << 16 | (y * 3) << 8 | 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "JPEG", baos);
        return baos.toByteArray();
    }

    private byte[] createPng(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                img.setRGB(x, y, 0xFF000000 | (x * 2) << 16 | (y * 2) << 8 | 80);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
