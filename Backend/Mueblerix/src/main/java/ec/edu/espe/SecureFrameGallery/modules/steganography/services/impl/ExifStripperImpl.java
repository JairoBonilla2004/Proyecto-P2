package ec.edu.espe.SecureFrameGallery.modules.steganography.services.impl;

import ec.edu.espe.SecureFrameGallery.modules.steganography.services.MetadataCleaner;
import ec.edu.espe.SecureFrameGallery.shared.utils.image.ImageIoUtil;
import ec.edu.espe.SecureFrameGallery.shared.utils.image.ImageReencodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;


@Service
@Slf4j
public class ExifStripperImpl implements MetadataCleaner {

    private static final long MAX_PIXELS = 64_000_000L;

    @Value("${app.sanitization.jpeg-quality:0.92}")
    private float jpegQuality;

    @Override
    public byte[] stripMetadata(byte[] imageBytes, String mimeType) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Los bytes de imagen no pueden estar vacíos");
        }

        ImageIoUtil.ensureInitialized();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("No se pudo decodificar la imagen — formato no soportado");
        }

        long pixels = (long) image.getWidth() * image.getHeight();
        if (pixels > MAX_PIXELS) {
            throw new IOException(String.format(
                    "Imagen demasiado grande para sanitización (%dx%d = %dMP — máximo 64MP)",
                    image.getWidth(), image.getHeight(), pixels / 1_000_000));
        }

        String format = ImageReencodeUtil.resolveOutputFormat(mimeType);
        float clampedQuality = ImageReencodeUtil.clampJpegQuality(jpegQuality);
        if ("jpeg".equals(format)) {
            log.debug("JPEG quality: {} (configured: {})", clampedQuality, jpegQuality);
        }

        byte[] cleanBytes = ImageReencodeUtil.reencode(image, format, clampedQuality);
        log.debug("Metadata stripping: {} → {} bytes (formato: {})",
                imageBytes.length, cleanBytes.length, format);

        if (cleanBytes.length > imageBytes.length * 3L) {
            log.warn("El archivo sanitizado es {}x más grande que el original — " +
                            "posible conversión de formato lossy→lossless (ej: WebP→PNG). " +
                            "Considere informar al usuario o almacenar el formato original.",
                    cleanBytes.length / Math.max(1, imageBytes.length));
        }

        return cleanBytes;
    }
}