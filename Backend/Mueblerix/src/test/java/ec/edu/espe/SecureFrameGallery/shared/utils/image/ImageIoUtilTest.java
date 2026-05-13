package ec.edu.espe.SecureFrameGallery.shared.utils.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageIoUtil — lectura y escritura segura de imágenes")
class ImageIoUtilTest {

    // ── Lectura ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Lee dimensiones de JPEG válido sin decodificar")
    void readsValidJpegSuccessfully() throws IOException {
        byte[] jpeg = createJpeg(64, 64);
        int[] dims = ImageIoUtil.readDimensionsWithoutDecoding(jpeg);
        assertArrayEquals(new int[]{64, 64}, dims);
    }

    @Test
    @DisplayName("Lee dimensiones de PNG válido sin decodificar")
    void readsValidPngSuccessfully() throws IOException {
        byte[] png = createPng(64, 64);
        int[] dims = ImageIoUtil.readDimensionsWithoutDecoding(png);
        assertArrayEquals(new int[]{64, 64}, dims);
    }

    @Test
    @DisplayName("Lee imagen y conserva dimensiones correctas")
    void readsImageWithCorrectDimensions() throws IOException {
        byte[] jpeg = createJpeg(120, 80);
        int[] dims = ImageIoUtil.readDimensionsWithoutDecoding(jpeg);
        assertArrayEquals(new int[]{120, 80}, dims);
    }

    @Test
    @DisplayName("Leer datos corruptos lanza excepción controlada")
    void readCorruptDataThrowsException() {
        byte[] corrupt = {0x00, 0x01, 0x02, 0x03, 0x04};
        assertThrows(IOException.class, () -> ImageIoUtil.readDimensionsWithoutDecoding(corrupt));
    }

    @Test
    @DisplayName("Leer null lanza excepción controlada")
    void readNullThrowsException() {
        assertThrows(Exception.class, () -> ImageIoUtil.readDimensionsWithoutDecoding(null));
    }

    @Test
    @DisplayName("Leer array vacío lanza excepción controlada")
    void readEmptyArrayThrowsException() {
        assertThrows(IOException.class, () -> ImageIoUtil.readDimensionsWithoutDecoding(new byte[]{}));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private byte[] createJpeg(int w, int h) throws IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                img.setRGB(x, y, (x * 3) << 16 | (y * 3) << 8 | 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "JPEG", baos);
        return baos.toByteArray();
    }

    private byte[] createPng(int w, int h) throws IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                img.setRGB(x, y, 0xFF000000 | (x * 2) << 16 | (y * 2) << 8 | 80);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
