package ec.edu.espe.SecureFrameGallery.modules.steganography.services.impl;

import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class LsbAnalyzerImplTest {

    @Test
    void cleanConstantPng_isClassifiedAsClean() throws IOException {
        LsbAnalyzerImpl analyzer = newConfiguredAnalyzer();

        BufferedImage clean = constantImage(512, 512, 128, 128, 128);
        byte[] bytes = toPngBytes(clean);

        var result = analyzer.analyze(bytes, "image/png");
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ImageStatus.CLEAN);
    }

    @Test
    void lsbReplacementInTopRegion_isNotClassifiedAsClean() throws IOException {
        LsbAnalyzerImpl analyzer = newConfiguredAnalyzer();

        BufferedImage base = constantImage(512, 512, 128, 128, 128);
        BufferedImage stego = embedRandomLsbInTopRows(base, 512 / 4, 1337L);
        byte[] bytes = toPngBytes(stego);

        var result = analyzer.analyze(bytes, "image/png");
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isNotEqualTo(ImageStatus.CLEAN);
    }

    private static LsbAnalyzerImpl newConfiguredAnalyzer() {
        LsbAnalyzerImpl analyzer = new LsbAnalyzerImpl();

        // Mirror production-like thresholds (but injected manually for unit test)
        ReflectionTestUtils.setField(analyzer, "minPixels", 262144);

        ReflectionTestUtils.setField(analyzer, "chiSquarePValueThreshold", 0.90);
        ReflectionTestUtils.setField(analyzer, "chiSquarePValueThresholdSynthetic", 0.94);

        ReflectionTestUtils.setField(analyzer, "deltaEntropyThreshold", 0.04);
        ReflectionTestUtils.setField(analyzer, "deltaEntropyHighThreshold", 0.07);

        ReflectionTestUtils.setField(analyzer, "smoothGradientThreshold", 12);
        ReflectionTestUtils.setField(analyzer, "minSmoothRatio", 0.05);
        ReflectionTestUtils.setField(analyzer, "regionalFraction", 0.05);

        ReflectionTestUtils.setField(analyzer, "sequentialTransitionThreshold", 0.46);
        ReflectionTestUtils.setField(analyzer, "sequentialDeltaThreshold", 0.04);

        ReflectionTestUtils.setField(analyzer, "lsbEntropyThreshold", 0.995);

        // Advanced/aggressive thresholds
        ReflectionTestUtils.setField(analyzer, "pixelCorrelationThreshold", 0.80);
        ReflectionTestUtils.setField(analyzer, "blockAnomalyRatio", 0.10);
        ReflectionTestUtils.setField(analyzer, "rangeDistortionThreshold", 0.30);
        ReflectionTestUtils.setField(analyzer, "noiseArtificialityThreshold", 0.60);
        ReflectionTestUtils.setField(analyzer, "fullScanMaxPixels", 1_000_000);

        return analyzer;
    }

    private static BufferedImage constantImage(int width, int height, int r, int g, int b) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    private static BufferedImage embedRandomLsbInTopRows(BufferedImage src, int topRows, long seed) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Random random = new Random(seed);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                if (y < topRows) {
                    r = (r & 0xFE) | random.nextInt(2);
                    g = (g & 0xFE) | random.nextInt(2);
                    b = (b & 0xFE) | random.nextInt(2);
                }

                int newRgb = (r << 16) | (g << 8) | b;
                out.setRGB(x, y, newRgb);
            }
        }

        return out;
    }

    private static byte[] toPngBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean ok = ImageIO.write(img, "png", out);
        if (!ok) throw new IOException("No ImageIO writer for PNG");
        return out.toByteArray();
    }
}
