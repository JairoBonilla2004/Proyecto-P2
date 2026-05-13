package ec.edu.espe.SecureFrameGallery.shared.utils.stego;

import java.awt.image.BufferedImage;


public final class StegoPixelStatsUtil {

    private StegoPixelStatsUtil() {}

    public record BitEntropyStats(double hBit0, double hBit1, double delta) {}

    public record ChiSquareResult(double pValue, long sampleCount) {}

    public record TransitionRate(double rate, long samples) {}

    public record SequentialPatternResult(boolean isAnomaly, double topRate, double bottomRate, int rowsAnalyzed) {}

    public static BitEntropyStats computeBitPlaneEntropyStats(
            BufferedImage img,
            boolean useSmoothOnly,
            int smoothGradientThreshold
    ) {
        int width = img.getWidth();
        int height = img.getHeight();
        long onesBit0 = 0;
        long onesBit1 = 0;
        long total = 0;
        int stride = pickStride(width, height);

        for (int y = 0; y < height; y += stride) {
            for (int x = 0; x < width; x += stride) {
                if (useSmoothOnly && !isSmoothPixel(img, x, y, smoothGradientThreshold)) continue;
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                onesBit0 += (r & 1) + (g & 1) + (b & 1);
                onesBit1 += ((r >> 1) & 1) + ((g >> 1) & 1) + ((b >> 1) & 1);
                total += 3;
            }
        }

        double h0 = shannonEntropyBernoulli(onesBit0, total);
        double h1 = shannonEntropyBernoulli(onesBit1, total);
        return new BitEntropyStats(h0, h1, h0 - h1);
    }

    public static double computeSmoothRatio(BufferedImage img, int smoothGradientThreshold) {
        int width = img.getWidth();
        int height = img.getHeight();
        if (width < 2 || height < 2) return 0.0;

        long smooth = 0;
        long total = 0;
        int stride = pickStride(width, height);

        for (int y = 1; y < height; y += stride) {
            for (int x = 1; x < width; x += stride) {
                int lum = fastLuma(img.getRGB(x, y));
                int lumL = fastLuma(img.getRGB(x - 1, y));
                int lumU = fastLuma(img.getRGB(x, y - 1));
                if (Math.abs(lum - lumL) + Math.abs(lum - lumU) <= smoothGradientThreshold) smooth++;
                total++;
            }
        }

        return total == 0 ? 0.0 : (double) smooth / total;
    }

    public static boolean isSmoothPixel(BufferedImage img, int x, int y, int smoothGradientThreshold) {
        if (x <= 0 || y <= 0 || x >= img.getWidth() || y >= img.getHeight()) return false;
        int lum = fastLuma(img.getRGB(x, y));
        int lumL = fastLuma(img.getRGB(x - 1, y));
        int lumU = fastLuma(img.getRGB(x, y - 1));
        return Math.abs(lum - lumL) + Math.abs(lum - lumU) <= smoothGradientThreshold;
    }

    public static ChiSquareResult computeChiSquarePValue(
            BufferedImage img,
            int fromRow,
            int toRow,
            boolean useSmoothOnly,
            int smoothGradientThreshold,
            long minChiSamples
    ) {
        long[] counts = new long[256];
        int width = img.getWidth();
        toRow = Math.min(toRow, img.getHeight());

        int stride = pickStride(img.getWidth(), toRow - fromRow);
        long totalSamples = 0;

        for (int y = fromRow; y < toRow; y += stride) {
            for (int x = 0; x < width; x += stride) {
                if (useSmoothOnly && !isSmoothPixel(img, x, y, smoothGradientThreshold)) continue;

                int rgb = img.getRGB(x, y);
                counts[(rgb >> 16) & 0xFF]++;
                counts[(rgb >> 8) & 0xFF]++;
                counts[rgb & 0xFF]++;
                totalSamples += 3;
            }
        }

        if (totalSamples < minChiSamples) {
            return new ChiSquareResult(0.0, totalSamples);
        }

        double chi = 0.0;
        int df = 0;
        for (int k = 0; k < 128; k++) {
            long n0 = counts[2 * k];
            long n1 = counts[2 * k + 1];
            long sum = n0 + n1;
            if (sum < 10) continue;
            double expected = sum / 2.0;
            chi += Math.pow(n0 - expected, 2) / expected;
            chi += Math.pow(n1 - expected, 2) / expected;
            df++;
        }

        if (df == 0) return new ChiSquareResult(0.0, totalSamples);

        double k = df;
        double z = (Math.cbrt(chi / k) - (1.0 - 2.0 / (9.0 * k)))
                / Math.sqrt(2.0 / (9.0 * k));
        double pUpper = 1.0 - normalCdf(z);
        return new ChiSquareResult(Math.max(0.0, Math.min(1.0, pUpper)), totalSamples);
    }

    public static SequentialPatternResult detectSequentialLsbPattern(
            BufferedImage img,
            double threshold,
            double sequentialDeltaThreshold,
            boolean useSmoothOnly,
            int smoothGradientThreshold
    ) {
        int height = img.getHeight();
        int regionRows = Math.max(5, height / 40);

        TransitionRate topRate = measureLsbTransitionRate(img, 0, regionRows, useSmoothOnly, smoothGradientThreshold);
        int controlStart = Math.max(regionRows, height - regionRows);
        TransitionRate bottomRate = measureLsbTransitionRate(img, controlStart, height, useSmoothOnly, smoothGradientThreshold);

        if (topRate.samples() < 10_000 || bottomRate.samples() < 10_000) {
            return new SequentialPatternResult(false, topRate.rate(), bottomRate.rate(), regionRows);
        }

        boolean topLooksRandom = topRate.rate() >= threshold;
        boolean deltaIsStrong = (topRate.rate() - bottomRate.rate()) >= sequentialDeltaThreshold;
        boolean isAnomaly = topLooksRandom && deltaIsStrong;

        return new SequentialPatternResult(isAnomaly, topRate.rate(), bottomRate.rate(), regionRows);
    }

    public static TransitionRate measureLsbTransitionRate(
            BufferedImage img,
            int fromRow,
            int toRow,
            boolean useSmoothOnly,
            int smoothGradientThreshold
    ) {
        int width = img.getWidth();
        toRow = Math.min(toRow, img.getHeight());
        if (fromRow >= toRow) return new TransitionRate(0.5, 0);

        long transitions = 0;
        long total = 0;
        int prev = -1;

        for (int y = fromRow; y < toRow; y++) {
            for (int x = 0; x < width; x++) {
                if (useSmoothOnly && !isSmoothPixel(img, x, y, smoothGradientThreshold)) continue;
                int rgb = img.getRGB(x, y);
                int[] channels = {(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
                for (int c : channels) {
                    int lsb = c & 1;
                    if (prev != -1) {
                        if (lsb != prev) transitions++;
                        total++;
                    }
                    prev = lsb;
                }
            }
        }

        return new TransitionRate(total == 0 ? 0.5 : (double) transitions / total, total);
    }

    public static int pickStride(int width, int height) {
        long px = (long) width * height;
        if (px > 8_000_000L) return 3;
        if (px > 2_000_000L) return 2;
        return 1;
    }

    private static int fastLuma(int rgb) {
        return (77 * ((rgb >> 16) & 0xFF) + 150 * ((rgb >> 8) & 0xFF) + 29 * (rgb & 0xFF)) >> 8;
    }

    private static double shannonEntropyBernoulli(long ones, long total) {
        if (total <= 0) return 0.0;
        double p1 = (double) ones / total;
        double p0 = 1.0 - p1;
        if (p1 == 0.0 || p0 == 0.0) return 0.0;
        return -(p1 * log2(p1) + p0 * log2(p0));
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    private static double normalCdf(double z) {
        return 0.5 * (1.0 + erf(z / Math.sqrt(2.0)));
    }

    private static double erf(double x) {
        double sign = x < 0 ? -1.0 : 1.0;
        double ax = Math.abs(x);
        double t = 1.0 / (1.0 + 0.5 * ax);
        double poly = 0.17087277;
        poly = poly * t - 0.82215223;
        poly = poly * t + 1.48851587;
        poly = poly * t - 1.13520398;
        poly = poly * t + 0.27886807;
        poly = poly * t - 0.18628806;
        poly = poly * t + 0.09678418;
        poly = poly * t + 0.37409196;
        poly = poly * t + 1.00002368;
        return sign * (1.0 - t * Math.exp(-ax * ax - 1.26551223 + t * poly));
    }
}
