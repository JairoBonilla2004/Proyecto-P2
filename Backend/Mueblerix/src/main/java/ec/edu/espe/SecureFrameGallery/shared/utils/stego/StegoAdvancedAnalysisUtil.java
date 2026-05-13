package ec.edu.espe.SecureFrameGallery.shared.utils.stego;

import java.awt.image.BufferedImage;

/**
 * Advanced steganography detection using pixel correlation, block analysis, and noise patterns.
 * Detects even the most subtle steganographic modifications.
 *
 * Responsibility: Aggressive detection techniques that complement LSB-focused analysis.
 */
public final class StegoAdvancedAnalysisUtil {

    private StegoAdvancedAnalysisUtil() {}

    // ── Record types ──────────────────────────────────────────────────────────
    public record CorrelationAnalysis(
        double horizontalCorrelation,
        double verticalCorrelation,
        double diagonalCorrelation,
        double averageCorrelation,
        boolean isAnomalous
    ) {}

    public record BlockAnomalyResult(
        int totalBlocks,
        int anomalousBlocks,
        double anomalyRatio,
        double maxBlockEntropy,
        boolean hasBlockAnomalies
    ) {}

    public record RangeDistortionResult(
        double minMaxRatio,
        double stdDevOfRanges,
        boolean hasDistortion,
        double distortionScore
    ) {}

    public record NoisePatternResult(
        double naturalNessScore,
        double artificiality,
        boolean looksArtificial,
        String characterization,
        double luminanceEntropyBits,
        double diffEntropyBits
    ) {}

    // ════════════════════════════════════════════════════════════════════════════
    // 1. PIXEL CORRELATION ANALYSIS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Analyzes correlation between adjacent pixels.
     * Natural images have HIGH correlation; steganographic noise reduces it.
     */
    public static CorrelationAnalysis analyzePixelCorrelation(BufferedImage img) {
        return analyzePixelCorrelation(img, Math.max(1, StegoPixelStatsUtil.pickStride(img.getWidth(), img.getHeight())), 0.80);
    }

    /**
     * Correlation analysis with explicit stride and threshold.
     * Use stride=1 for true pixel-by-pixel scanning (more expensive).
     */
    public static CorrelationAnalysis analyzePixelCorrelation(BufferedImage img, int stride, double anomalyThreshold) {
        int w = img.getWidth();
        int h = img.getHeight();
        int safeStride = Math.max(1, stride);

        double sumH = 0.0, countH = 0.0;
        double sumV = 0.0, countV = 0.0;
        double sumD = 0.0, countD = 0.0;

        // Horizontal correlation
        for (int y = 0; y < h; y += safeStride) {
            for (int x = 1; x < w; x += safeStride) {
                int lum1 = fastLuma(img.getRGB(x - 1, y));
                int lum2 = fastLuma(img.getRGB(x, y));
                double diff = Math.abs(lum1 - lum2);
                sumH += 1.0 - (diff / 255.0);
                countH++;
            }
        }

        // Vertical correlation
        for (int y = 1; y < h; y += safeStride) {
            for (int x = 0; x < w; x += safeStride) {
                int lum1 = fastLuma(img.getRGB(x, y - 1));
                int lum2 = fastLuma(img.getRGB(x, y));
                double diff = Math.abs(lum1 - lum2);
                sumV += 1.0 - (diff / 255.0);
                countV++;
            }
        }

        // Diagonal correlation
        for (int y = 1; y < h; y += safeStride) {
            for (int x = 1; x < w; x += safeStride) {
                int lum1 = fastLuma(img.getRGB(x - 1, y - 1));
                int lum2 = fastLuma(img.getRGB(x, y));
                double diff = Math.abs(lum1 - lum2);
                sumD += 1.0 - (diff / 255.0);
                countD++;
            }
        }

        double corrH = countH > 0 ? sumH / countH : 0.5;
        double corrV = countV > 0 ? sumV / countV : 0.5;
        double corrD = countD > 0 ? sumD / countD : 0.5;
        double avg = (corrH + corrV + corrD) / 3.0;

        boolean isAnomalous = avg < anomalyThreshold;

        return new CorrelationAnalysis(corrH, corrV, corrD, avg, isAnomalous);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 2. BLOCK-LEVEL ANOMALY DETECTION
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Divides image into blocks and detects localized entropy/pattern anomalies.
     * Steganography often creates suspicious entropy peaks in certain regions.
     */
    public static BlockAnomalyResult detectBlockAnomalies(BufferedImage img) {
        int blockSize = 16;
        int w = img.getWidth();
        int h = img.getHeight();

        int blockCountX = (w + blockSize - 1) / blockSize;
        int blockCountY = (h + blockSize - 1) / blockSize;
        int totalBlocks = blockCountX * blockCountY;

        double[] blockEntropies = new double[totalBlocks];
        int blockIdx = 0;

        for (int by = 0; by < blockCountY; by++) {
            for (int bx = 0; bx < blockCountX; bx++) {
                int yStart = by * blockSize;
                int xStart = bx * blockSize;
                int yEnd = Math.min(yStart + blockSize, h);
                int xEnd = Math.min(xStart + blockSize, w);

                double entropy = computeBlockEntropy(img, xStart, yStart, xEnd, yEnd);
                blockEntropies[blockIdx] = entropy;
                blockIdx++;
            }
        }

        // Statistical analysis of block entropies
        double mean = 0.0;
        double max = 0.0;
        for (double e : blockEntropies) {
            mean += e;
            max = Math.max(max, e);
        }
        mean /= blockEntropies.length;

        double variance = 0.0;
        for (double e : blockEntropies) {
            variance += Math.pow(e - mean, 2);
        }
        variance /= blockEntropies.length;
        double stdDev = Math.sqrt(variance);

        // Detect blocks with suspiciously high entropy (outliers)
        double threshold = mean + 1.5 * stdDev;
        int anomalousBlocks = 0;
        for (double e : blockEntropies) {
            if (e > threshold) {
                anomalousBlocks++;
            }
        }

        double anomalyRatio = (double) anomalousBlocks / totalBlocks;
        boolean hasAnomalies = anomalyRatio > 0.10 || max > (mean + 2.0 * stdDev);

        return new BlockAnomalyResult(totalBlocks, anomalousBlocks, anomalyRatio, max, hasAnomalies);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 3. RANGE DISTORTION DETECTION
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Analyzes min/max value ranges in image blocks.
     * Steganography can distort dynamic range locally or globally.
     */
    public static RangeDistortionResult detectRangeDistortion(BufferedImage img) {
        int blockSize = 8;
        int w = img.getWidth();
        int h = img.getHeight();

        int blockCountX = (w + blockSize - 1) / blockSize;
        int blockCountY = (h + blockSize - 1) / blockSize;

        double[] ranges = new double[blockCountX * blockCountY];
        int blockIdx = 0;

        for (int by = 0; by < blockCountY; by++) {
            for (int bx = 0; bx < blockCountX; bx++) {
                int yStart = by * blockSize;
                int xStart = bx * blockSize;
                int yEnd = Math.min(yStart + blockSize, h);
                int xEnd = Math.min(xStart + blockSize, w);

                int minVal = 255;
                int maxVal = 0;

                for (int y = yStart; y < yEnd; y++) {
                    for (int x = xStart; x < xEnd; x++) {
                        int lum = fastLuma(img.getRGB(x, y));
                        minVal = Math.min(minVal, lum);
                        maxVal = Math.max(maxVal, lum);
                    }
                }

                ranges[blockIdx] = (double) (maxVal - minVal);
                blockIdx++;
            }
        }

        // Calculate statistics
        double meanRange = 0.0;
        for (double r : ranges) {
            meanRange += r;
        }
        meanRange /= ranges.length;

        double variance = 0.0;
        for (double r : ranges) {
            variance += Math.pow(r - meanRange, 2);
        }
        variance /= ranges.length;
        double stdDev = Math.sqrt(variance);

        // Min-max ratio: for natural images, min ranges appear; steganography flattens
        double minMaxRatio = (meanRange > 0) ? (stdDev / meanRange) : 0.0;

        // If ratio is suspiciously low, ranges are too uniform
        boolean hasDistortion = minMaxRatio < 0.30 || minMaxRatio > 1.20;
        double distortionScore = Math.abs(minMaxRatio - 0.50);

        return new RangeDistortionResult(minMaxRatio, stdDev, hasDistortion, distortionScore);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // 4. NOISE PATTERN CHARACTERIZATION
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Determines if noise patterns look natural or artificial.
     * Steganographic noise tends to be more uniform/random than natural image noise.
     */
    public static NoisePatternResult characterizeNoisePattern(BufferedImage img) {
        return characterizeNoisePattern(img, Math.max(1, StegoPixelStatsUtil.pickStride(img.getWidth(), img.getHeight())));
    }

    /**
     * Noise characterization with explicit stride.
     */
    public static NoisePatternResult characterizeNoisePattern(BufferedImage img, int stride) {
        int w = img.getWidth();
        int h = img.getHeight();
        int safeStride = Math.max(1, stride);

        long[] lumHistogram = new long[256];
        long[] diffHistogram = new long[256];

        for (int y = 0; y < h; y += safeStride) {
            for (int x = 0; x < w; x += safeStride) {
                int lum = fastLuma(img.getRGB(x, y));
                lumHistogram[lum]++;

                if (x > 0) {
                    int prevLum = fastLuma(img.getRGB(x - 1, y));
                    int diff = Math.abs(lum - prevLum);
                    diffHistogram[diff]++;
                }
            }
        }

        double lumEntropyBits = computeHistogramEntropy(lumHistogram);
        double diffEntropyBits = computeHistogramEntropy(diffHistogram);

        // Normalize entropies to [0,1] (max 8 bits for 256 bins)
        double lumEntropyN = clamp01(lumEntropyBits / 8.0);
        double diffEntropyN = clamp01(diffEntropyBits / 8.0);

        // Natural images: lower diff-entropy than pure random noise; stego pushes it upward.
        // We use a simple heuristic score in [0,1].
        double naturalness = clamp01((1.0 - diffEntropyN) * 0.75 + (1.0 - lumEntropyN) * 0.25);
        double artificiality = clamp01(1.0 - naturalness);

        // Very high diff entropy (close to uniform diffs) is suspicious.
        boolean looksArtificial = diffEntropyBits > 6.5 || (lumEntropyBits < 4.0 && diffEntropyBits > 5.5);

        String characterization = looksArtificial
            ? "Artificial noise pattern (likely steganography)"
            : "Natural image noise";

        return new NoisePatternResult(naturalness, artificiality, looksArtificial, characterization, lumEntropyBits, diffEntropyBits);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ════════════════════════════════════════════════════════════════════════════

    private static double computeBlockEntropy(BufferedImage img, int xStart, int yStart, int xEnd, int yEnd) {
        long[] histogram = new long[256];

        for (int y = yStart; y < yEnd; y++) {
            for (int x = xStart; x < xEnd; x++) {
                int lum = fastLuma(img.getRGB(x, y));
                histogram[lum]++;
            }
        }

        return computeHistogramEntropy(histogram);
    }

    private static double computeHistogramEntropy(long[] histogram) {
        long total = 0;
        for (long count : histogram) {
            total += count;
        }

        if (total == 0) return 0.0;

        double entropy = 0.0;
        for (long count : histogram) {
            if (count > 0) {
                double p = (double) count / total;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }

        return entropy;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static int fastLuma(int rgb) {
        return (77 * ((rgb >> 16) & 0xFF) + 150 * ((rgb >> 8) & 0xFF) + 29 * (rgb & 0xFF)) >> 8;
    }
}
