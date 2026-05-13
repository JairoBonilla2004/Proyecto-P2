package ec.edu.espe.SecureFrameGallery.modules.steganography.services.impl;

import ec.edu.espe.SecureFrameGallery.modules.steganography.dtos.StegoAnalysisResultDto;
import ec.edu.espe.SecureFrameGallery.modules.steganography.services.ImageAnalyzer;
import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
import ec.edu.espe.SecureFrameGallery.shared.utils.image.ImageIoUtil;
import ec.edu.espe.SecureFrameGallery.shared.utils.stego.StegoAdvancedAnalysisUtil;
import ec.edu.espe.SecureFrameGallery.shared.utils.stego.StegoEofUtil;
import ec.edu.espe.SecureFrameGallery.shared.utils.stego.StegoImageTypeClassifier;
import ec.edu.espe.SecureFrameGallery.shared.utils.stego.StegoPixelStatsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class LsbAnalyzerImpl implements ImageAnalyzer {

    private static final int MAX_WIDTH  = 8000;
    private static final int MAX_HEIGHT = 8000;
    private static final int MAX_PIXELS = MAX_WIDTH * MAX_HEIGHT;

    private static final long MIN_CHI_SAMPLES = 50_000L;

    @Value("${app.steganography.min-pixels:262144}")
    private int minPixels;

    @Value("${app.steganography.chi-square-pvalue-threshold:0.95}")
    private double chiSquarePValueThreshold;

    @Value("${app.steganography.chi-square-pvalue-threshold-synthetic:0.98}")
    private double chiSquarePValueThresholdSynthetic;

    @Value("${app.steganography.delta-entropy-threshold:0.60}")
    private double deltaEntropyThreshold;

    @Value("${app.steganography.delta-entropy-high-threshold:0.12}")
    private double deltaEntropyHighThreshold;

    @Value("${app.steganography.lsb-entropy-threshold:0.995}")
    private double lsbEntropyThreshold;

    @Value("${app.steganography.smooth-gradient-threshold:12}")
    private int smoothGradientThreshold;

    @Value("${app.steganography.min-smooth-ratio:0.05}")
    private double minSmoothRatio;

    @Value("${app.steganography.regional-fraction:0.05}")
    private double regionalFraction;

    @Value("${app.steganography.sequential-transition-threshold:0.34}")
    private double sequentialTransitionThreshold;

    @Value("${app.steganography.sequential-delta-threshold:0.04}")
    private double sequentialDeltaThreshold;

    @Value("${app.steganography.pixel-correlation-threshold:0.80}")
    private double pixelCorrelationThreshold;

    @Value("${app.steganography.block-anomaly-ratio:0.10}")
    private double blockAnomalyRatio;

    @Value("${app.steganography.range-distortion-threshold:0.30}")
    private double rangeDistortionThreshold;

    @Value("${app.steganography.noise-artificality-threshold:0.60}")
    private double noiseArtificialityThreshold;

    @Value("${app.steganography.full-scan-max-pixels:1000000}")
    private int fullScanMaxPixels;

    @Override
    public StegoAnalysisResultDto analyze(byte[] imageBytes, String mimeType) {
        List<String> reasons = new ArrayList<>();

        boolean eofAnomaly        = false;
        boolean chiGlobalFlag     = false;
        boolean chiRegionalFlag   = false;
        boolean deltaFlag         = false;
        boolean sequentialFlag    = false;
        boolean lsbEntropyFlag    = false;
        
        // Nuevos detectores agresivos
        boolean correlationFlag   = false;
        boolean blockAnomalyFlag  = false;
        boolean rangeDistortionFlag = false;
        boolean noiseArtificialFlag = false;

        double lsbEntropyScore    = 0.0;
        double chiGlobalPValue    = 0.0;
        double chiRegionalPValue  = 0.0;
        double deltaEntropy       = 0.0;
        double bit1Entropy        = 0.0;

        try {
            int[] dims = ImageIoUtil.readDimensionsWithoutDecoding(imageBytes);
            int w = dims[0], h = dims[1];
            if (w > MAX_WIDTH || h > MAX_HEIGHT || (long) w * h > MAX_PIXELS) {
                reasons.add(String.format(
                        "Imagen demasiado grande para análisis (%dx%d — máximo %dx%d)",
                        w, h, MAX_WIDTH, MAX_HEIGHT));
                return StegoAnalysisResultDto.builder()
                        .status(ImageStatus.SUSPICIOUS)
                        .lsbEntropyScore(0.0)
                        .eofAnomalyFound(false)
                        .detectionReasons(reasons)
                        .build();
            }
        } catch (IOException e) {
            log.warn("No se pudieron leer las dimensiones sin decodificar: {}", e.getMessage());
        }

        eofAnomaly = StegoEofUtil.detectEofAnomaly(imageBytes, mimeType);
        if (eofAnomaly) {
            reasons.add("Datos extra detectados después del marcador EOF de la imagen");
        }

        try {
            ImageIoUtil.ensureInitialized();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));

            if (img != null) {
                int pixels = img.getWidth() * img.getHeight();

                if (pixels >= minPixels) {
                    StegoImageTypeClassifier.Classification classification =
                        StegoImageTypeClassifier.classify(img, mimeType);
                    StegoImageTypeClassifier.ImageType imageType = classification.type();

                    log.debug("Image classification: uniqueColors={}, sampleCount={}, solidRowRatio={}, colorDensity={}",
                        classification.uniqueColors(), classification.sampleCount(),
                        classification.solidRowRatio(), classification.colorDensity());
                    
                    double chiThreshold = (imageType == StegoImageTypeClassifier.ImageType.SYNTHETIC)
                            ? chiSquarePValueThresholdSynthetic
                            : chiSquarePValueThreshold;

                    log.debug("Image type classified as: {}, chi threshold: {}",
                            imageType, chiThreshold);

                    // ── Análisis tradicionales LSB ─────────────────────────────────────
                    double smoothRatio = StegoPixelStatsUtil.computeSmoothRatio(img, smoothGradientThreshold);
                    boolean useSmoothOnly = smoothRatio >= minSmoothRatio;

                    StegoPixelStatsUtil.BitEntropyStats bes =
                            StegoPixelStatsUtil.computeBitPlaneEntropyStats(img, useSmoothOnly, smoothGradientThreshold);
                    lsbEntropyScore = bes.hBit0();
                    bit1Entropy     = bes.hBit1();
                    deltaEntropy    = Math.max(0.0, bes.delta());

                    lsbEntropyFlag = lsbEntropyScore >= lsbEntropyThreshold;

                    log.debug("LSB entropy bit0={}, bit1={}, delta={}, smoothRatio={}, useSmoothOnly={}",
                        lsbEntropyScore, bit1Entropy, deltaEntropy, smoothRatio, useSmoothOnly);

                    boolean bit1HasEnoughVariance = bit1Entropy >= 0.70;
                    if (lsbEntropyFlag && deltaEntropy >= deltaEntropyThreshold && bit1HasEnoughVariance) {
                        deltaFlag = true;
                        reasons.add(String.format(
                            "Delta entropía bit0−bit1 elevada (%.4f >= %.4f) con LSB entropy=%.4f y bit1 entropy=%.4f — " +
                                "patrón compatible con LSB replacement",
                            deltaEntropy, deltaEntropyThreshold, lsbEntropyScore, bit1Entropy));
                    }

                    if (!"image/jpeg".equals(mimeType)) {
                        StegoPixelStatsUtil.ChiSquareResult globalChi = StegoPixelStatsUtil.computeChiSquarePValue(
                            img, 0, img.getHeight(), false, smoothGradientThreshold, MIN_CHI_SAMPLES);
                        chiGlobalPValue = globalChi.pValue();

                        if (globalChi.sampleCount() >= MIN_CHI_SAMPLES
                                && chiGlobalPValue >= chiThreshold) {
                            chiGlobalFlag = true;
                            reasons.add(String.format(
                                    "Chi-square global alto (p=%.4f >= %.4f, n=%d) — " +
                                            "pares (2k,2k+1) igualados en toda la imagen",
                                    chiGlobalPValue, chiThreshold, globalChi.sampleCount()));
                        } else if (globalChi.sampleCount() < MIN_CHI_SAMPLES) {
                            log.debug("Chi-square global omitido: muestras insuficientes ({})",
                                    globalChi.sampleCount());
                        }
                    } else {
                        log.debug("Chi-square global omitido para JPEG (compresión lossy invalida el test)");
                    }

                    double regionalChiThreshold = "image/jpeg".equals(mimeType)
                            ? Math.min(0.99, chiThreshold + 0.02)
                            : chiThreshold;
                    int regionRows = Math.max(10, (int) (img.getHeight() * regionalFraction));
                    int minRowsForChi = (int) Math.ceil((double) MIN_CHI_SAMPLES / 3.0 / Math.max(1, img.getWidth()));
                    regionRows = Math.max(regionRows, minRowsForChi);
                    regionRows = Math.min(regionRows, img.getHeight());
                    StegoPixelStatsUtil.ChiSquareResult regionalChi = StegoPixelStatsUtil.computeChiSquarePValue(
                        img, 0, regionRows, false, smoothGradientThreshold, MIN_CHI_SAMPLES);
                    chiRegionalPValue = regionalChi.pValue();

                    if (regionalChi.sampleCount() >= MIN_CHI_SAMPLES
                            && chiRegionalPValue >= regionalChiThreshold) {
                        chiRegionalFlag = true;
                        reasons.add(String.format(
                                "Chi-square regional alto en primeras %d filas (p=%.4f >= %.4f, n=%d) — " +
                                        "posible escritura secuencial LSB",
                                regionRows, chiRegionalPValue, regionalChiThreshold,
                                regionalChi.sampleCount()));
                    } else if (regionalChi.sampleCount() < MIN_CHI_SAMPLES) {
                        log.debug("Chi-square regional omitido: muestras insuficientes ({}) en {} filas",
                                regionalChi.sampleCount(), regionRows);
                    }

                    double adaptiveSeqThreshold = sequentialTransitionThreshold;
                    if (smoothRatio >= 0.40) {
                        adaptiveSeqThreshold = Math.min(0.49, sequentialTransitionThreshold + 0.01);
                        log.debug("Smooth image (ratio={}): adjusted sequential threshold to {}",
                                smoothRatio, adaptiveSeqThreshold);
                    }
                    StegoPixelStatsUtil.SequentialPatternResult seqResult =
                            StegoPixelStatsUtil.detectSequentialLsbPattern(
                                img,
                                adaptiveSeqThreshold,
                                sequentialDeltaThreshold,
                                useSmoothOnly,
                                smoothGradientThreshold);

                    log.debug("LSB transition rates: top={}, bottom={}, thresholdTop>={}, deltaThreshold={}, useSmoothOnly={}",
                        seqResult.topRate(), seqResult.bottomRate(), adaptiveSeqThreshold,
                        sequentialDeltaThreshold, useSmoothOnly);
                    sequentialFlag = seqResult.isAnomaly();
                    if (sequentialFlag) {
                        reasons.add(String.format(
                            "Tasa de transición LSB anómala (top=%.4f, bottom=%.4f, umbral top>=%.4f, Δ>=%.4f) en primeras %d filas " +
                                "— posible escritura secuencial/region-based de payload",
                            seqResult.topRate(), seqResult.bottomRate(), adaptiveSeqThreshold, sequentialDeltaThreshold,
                            seqResult.rowsAnalyzed()));
                    }

                    // ── NUEVOS DETECTORES AGRESIVOS ─────────────────────────────────────
                    log.debug("Iniciando análisis avanzado de esteganografía...");

                        int advancedStride = pixels <= fullScanMaxPixels
                            ? 1
                            : Math.max(1, StegoPixelStatsUtil.pickStride(img.getWidth(), img.getHeight()));
                        log.debug("Advanced analysis stride={} (pixels={}, fullScanMaxPixels={})",
                            advancedStride, pixels, fullScanMaxPixels);

                    // 1. Correlación de píxeles vecinos
                    StegoAdvancedAnalysisUtil.CorrelationAnalysis corrAnalysis =
                            StegoAdvancedAnalysisUtil.analyzePixelCorrelation(img, advancedStride, pixelCorrelationThreshold);
                        correlationFlag = corrAnalysis.averageCorrelation() < pixelCorrelationThreshold;
                    if (correlationFlag) {
                        reasons.add(String.format(
                            "Correlación anómala de píxeles vecinos (avg=%.4f < %.4f) — " +
                                "H=%.4f, V=%.4f, D=%.4f — indica perturbación de píxeles",
                            corrAnalysis.averageCorrelation(), pixelCorrelationThreshold,
                            corrAnalysis.horizontalCorrelation(), corrAnalysis.verticalCorrelation(),
                            corrAnalysis.diagonalCorrelation()));
                    }

                    // 2. Anomalías de entropía en bloques
                    StegoAdvancedAnalysisUtil.BlockAnomalyResult blockAnalysis =
                            StegoAdvancedAnalysisUtil.detectBlockAnomalies(img);
                    blockAnomalyFlag = blockAnalysis.anomalyRatio() >= blockAnomalyRatio || blockAnalysis.hasBlockAnomalies();
                    if (blockAnomalyFlag) {
                        reasons.add(String.format(
                            "Anomalías de entropía en bloques detectadas: %d de %d bloques (%.2f%%) con entropía > umbral — " +
                                "max entropy=%.4f — patrón típico de esteganografía",
                            blockAnalysis.anomalousBlocks(), blockAnalysis.totalBlocks(),
                            blockAnalysis.anomalyRatio() * 100.0, blockAnalysis.maxBlockEntropy()));
                    }

                    // 3. Distorsión de rango dinámico
                    StegoAdvancedAnalysisUtil.RangeDistortionResult rangeAnalysis =
                            StegoAdvancedAnalysisUtil.detectRangeDistortion(img);
                        rangeDistortionFlag = rangeAnalysis.minMaxRatio() < rangeDistortionThreshold
                            || rangeAnalysis.minMaxRatio() > 1.20
                            || rangeAnalysis.hasDistortion();
                    if (rangeDistortionFlag) {
                        reasons.add(String.format(
                            "Distorsión de rango dinámico detectada (ratio=%.4f, distortionScore=%.4f) — " +
                                "rangos min-max irregulares indican embedding de datos",
                            rangeAnalysis.minMaxRatio(), rangeAnalysis.distortionScore()));
                    }

                    // 4. Patrón de ruido artificial
                    StegoAdvancedAnalysisUtil.NoisePatternResult noiseAnalysis =
                            StegoAdvancedAnalysisUtil.characterizeNoisePattern(img, advancedStride);
                    noiseArtificialFlag = noiseAnalysis.artificiality() >= noiseArtificialityThreshold
                            || noiseAnalysis.looksArtificial();
                    if (noiseArtificialFlag) {
                        boolean thresholdTriggered = noiseAnalysis.artificiality() >= noiseArtificialityThreshold;
                        reasons.add(String.format(
                            "%s (%s) — naturalness=%.4f, artificiality=%.4f (umbral=%.4f; Hlum=%.2f, Hdiff=%.2f) — coherente con embedding de datos",
                            thresholdTriggered
                                ? "Artificialidad de ruido por encima del umbral"
                                : "Patrón de ruido artificial detectado",
                            noiseAnalysis.characterization(),
                            noiseAnalysis.naturalNessScore(), noiseAnalysis.artificiality(),
                            noiseArtificialityThreshold,
                            noiseAnalysis.luminanceEntropyBits(), noiseAnalysis.diffEntropyBits()));
                    }

                } else {
                    log.debug("Imagen demasiado pequeña para análisis estadístico: {}x{}",
                            img.getWidth(), img.getHeight());
                }
            }

        } catch (IOException e) {
            log.warn("Error al analizar píxeles: {}", e.getMessage());
            reasons.add("Error al procesar los píxeles de la imagen");
        }

        ImageStatus status = determineStatus(
                eofAnomaly, chiGlobalFlag, chiRegionalFlag,
                deltaFlag, sequentialFlag, lsbEntropyFlag,
                correlationFlag, blockAnomalyFlag, rangeDistortionFlag, noiseArtificialFlag,
                deltaEntropy, lsbEntropyScore, bit1Entropy,
                chiGlobalPValue, chiRegionalPValue
        );

        log.info("Stego analysis complete: status={}, eof={}, chiGlobal={}, chiRegional={}, " +
                "delta={}, sequential={}, correlation={}, blockAnomaly={}, rangeDistortion={}, noiseArtificial={}",
                status, eofAnomaly, chiGlobalFlag, chiRegionalFlag,
                deltaFlag, sequentialFlag, correlationFlag, blockAnomalyFlag, rangeDistortionFlag, noiseArtificialFlag);

        return StegoAnalysisResultDto.builder()
                .status(status)
                .lsbEntropyScore(lsbEntropyScore)
                .eofAnomalyFound(eofAnomaly)
                .detectionReasons(reasons)
                .build();
    }

    private ImageStatus determineStatus(
            boolean eofAnomaly,
            boolean chiGlobalFlag,
            boolean chiRegionalFlag,
            boolean deltaFlag,
            boolean sequentialFlag,
            boolean lsbEntropyFlag,
            boolean correlationFlag,
            boolean blockAnomalyFlag,
            boolean rangeDistortionFlag,
            boolean noiseArtificialFlag,
            double deltaEntropy,
            double lsbEntropy,
            double bit1Entropy,
            double chiGlobalPValue,
            double chiRegionalPValue
    ) {
        if (eofAnomaly) return ImageStatus.POSITIVE;

        int risk = 0;

        // Puntuación agresiva: cada detector cuenta mucho
        if (chiRegionalFlag) risk += 3;
        if (chiGlobalFlag)   risk += 2;
        if (sequentialFlag)  risk += 2;
        if (deltaFlag)       risk += 1;

        // LSB entropy (por sí sola es común en imágenes naturales), pero en modo estricto aporta señal leve
        if (lsbEntropyFlag)  risk += 1;
        
        // Nuevos detectores agresivos: cuentan mucho
        if (correlationFlag) risk += 2;
        if (blockAnomalyFlag) risk += 2;
        if (rangeDistortionFlag) risk += 1;
        if (noiseArtificialFlag) risk += 2;

        // Combinaciones “no debería pasar”: alta aleatoriedad LSB + ruido artificial => muy sospechoso
        if (lsbEntropyFlag && noiseArtificialFlag) {
            risk += 1;
            log.debug("LSB entropy + artificial noise co-occur: strong suspicion");
        }

        if (lsbEntropyFlag && deltaEntropy >= deltaEntropyHighThreshold) {
            risk += 1;
            log.debug("Strong delta entropy bump: delta={}, highThreshold={}",
                deltaEntropy, deltaEntropyHighThreshold);
        }

        if (lsbEntropyFlag && (chiRegionalFlag || chiGlobalFlag || sequentialFlag || deltaFlag)) {
            risk += 1;
            log.debug("LSB entropy supporting point applied: lsbEntropy={}, threshold={}",
                lsbEntropy, lsbEntropyThreshold);
        }

        boolean hasStatisticalSignal = chiRegionalFlag || chiGlobalFlag;
        boolean hasNonChiSignal      = deltaFlag || sequentialFlag || lsbEntropyFlag || correlationFlag || blockAnomalyFlag;
        if (hasStatisticalSignal && hasNonChiSignal) {
            risk += 1;
            log.debug("Combination bonus applied: chi-square + (delta/sequential/entropy/correlation/block) co-occur");
        }

        // Bonus si múltiples detectores agresivos coinciden
        int advancedDetections = (correlationFlag ? 1 : 0) + (blockAnomalyFlag ? 1 : 0) + 
                                 (rangeDistortionFlag ? 1 : 0) + (noiseArtificialFlag ? 1 : 0);
        if (advancedDetections >= 2) {
            risk += 2;
            log.debug("Multiple advanced detections ({}/4) co-occur: very suspicious", advancedDetections);
        }

        log.debug("Risk score: {} (chiRegional={}, chiGlobal={}, sequential={}, delta={}, correlation={}, " +
                "blockAnomaly={}, rangeDistortion={}, noiseArtificial={})",
            risk, chiRegionalFlag, chiGlobalFlag, sequentialFlag, deltaFlag, correlationFlag,
            blockAnomalyFlag, rangeDistortionFlag, noiseArtificialFlag);

        // Thresholds: más agresivos ahora que tenemos múltiples detectores
        if (risk >= 4) return ImageStatus.POSITIVE;
        if (risk >= 2) return ImageStatus.SUSPICIOUS;
        return ImageStatus.CLEAN;
    }
}
