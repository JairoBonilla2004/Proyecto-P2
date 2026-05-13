package ec.edu.espe.SecureFrameGallery.shared.utils.image;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Image re-encoding helpers.
 * Responsibility: re-encode images into a fresh raster (metadata stripped by construction).
 */
public final class ImageReencodeUtil {

    private ImageReencodeUtil() {}

    public static String resolveOutputFormat(String mimeType) {
        if (mimeType == null) return "png";
        return switch (mimeType) {
            case "image/jpeg" -> "jpeg";
            case "image/png" -> "png";
            case "image/webp" -> "png";
            case "image/gif" -> "png";
            default -> "png";
        };
    }

    public static byte[] reencode(BufferedImage image, String outputFormat, float jpegQuality) throws IOException {
        ImageIoUtil.ensureInitialized();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        if ("jpeg".equals(outputFormat)) {
            writeJpeg(image, out, jpegQuality);
        } else {
            BufferedImage normalized = normalizeToArgb(image);
            boolean written = ImageIO.write(normalized, outputFormat, out);
            if (!written) {
                throw new IOException("No hay ImageWriter disponible para formato: " + outputFormat);
            }
        }

        return out.toByteArray();
    }

    public static float clampJpegQuality(float jpegQuality) {
        return Math.max(0.0f, Math.min(jpegQuality, 1.0f));
    }

    private static void writeJpeg(BufferedImage source, ByteArrayOutputStream out, float jpegQuality) throws IOException {
        BufferedImage rgb = flattenAlpha(source);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IOException("No hay ImageWriter disponible para JPEG");
        ImageWriter writer = writers.next();

        float clampedQuality = clampJpegQuality(jpegQuality);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(clampedQuality);
            }
            writer.write(null, new IIOImage(rgb, null, null), param);
            ios.flush();
        } finally {
            writer.dispose();
        }
    }

    private static BufferedImage flattenAlpha(BufferedImage source) {
        if (!source.getColorModel().hasAlpha()) return source;

        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rgb.createGraphics();
        try {
            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g2d.drawImage(source, 0, 0, null);
        } finally {
            g2d.dispose();
        }
        return rgb;
    }

    private static BufferedImage normalizeToArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) return source;

        BufferedImage argb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = argb.createGraphics();
        try {
            g2d.drawImage(source, 0, 0, null);
        } finally {
            g2d.dispose();
        }
        return argb;
    }
}
