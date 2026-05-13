package ec.edu.espe.SecureFrameGallery.shared.utils.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Shared ImageIO helpers.
 * Responsibility: provide safe, reusable ImageIO initialization and lightweight header reads.
 */
public final class ImageIoUtil {

    private static final Object IMAGEIO_INIT_LOCK = new Object();
    private static volatile boolean imageIoInitialized = false;

    private ImageIoUtil() {}

    public static void ensureInitialized() {
        if (imageIoInitialized) return;
        synchronized (IMAGEIO_INIT_LOCK) {
            if (imageIoInitialized) return;
            ImageIO.scanForPlugins();
            imageIoInitialized = true;
        }
    }

    public static int[] readDimensionsWithoutDecoding(byte[] imageBytes) throws IOException {
        ensureInitialized();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) throw new IOException("Formato no reconocido");
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        }
    }
}
