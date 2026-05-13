package ec.edu.espe.SecureFrameGallery.shared.utils.stego;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

public final class StegoImageTypeClassifier {

    private StegoImageTypeClassifier() {}

    public enum ImageType { PHOTOGRAPHIC, SYNTHETIC }

    public record Classification(ImageType type, int uniqueColors, int sampleCount, double solidRowRatio, double colorDensity) {}

    public static Classification classify(BufferedImage img, String mimeType) {
        if (img == null) {
            return new Classification(ImageType.PHOTOGRAPHIC, 0, 0, 0.0, 0.0);
        }

        if ("image/jpeg".equals(mimeType)) {
            return new Classification(ImageType.PHOTOGRAPHIC, 0, 0, 0.0, 0.0);
        }

        int w = img.getWidth();
        int h = img.getHeight();
        int stride = StegoPixelStatsUtil.pickStride(w, h);
        int sampleCount = 0;

        Set<Integer> uniqueColors = new HashSet<>();
        long solidRowCount = 0;

        for (int y = 0; y < h; y += stride) {
            int firstPixel = img.getRGB(0, y);
            boolean isSolidRow = true;

            for (int x = 0; x < w; x += stride) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                uniqueColors.add(rgb);
                sampleCount++;
                if ((img.getRGB(x, y) & 0xFFFFFF) != (firstPixel & 0xFFFFFF)) {
                    isSolidRow = false;
                }
            }
            if (isSolidRow) solidRowCount++;
        }

        int sampledRows = h / stride;
        double solidRowRatio = sampledRows > 0 ? (double) solidRowCount / sampledRows : 0;
        double colorDensity = sampleCount > 0 ? (double) uniqueColors.size() / sampleCount : 0;

        ImageType type;
        if (solidRowRatio >= 0.15) {
            type = ImageType.SYNTHETIC;
        } else if (colorDensity < 0.005 && uniqueColors.size() < 256) {
            type = ImageType.SYNTHETIC;
        } else {
            type = ImageType.PHOTOGRAPHIC;
        }

        return new Classification(type, uniqueColors.size(), sampleCount, solidRowRatio, colorDensity);
    }
}
