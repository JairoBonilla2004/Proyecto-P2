package ec.edu.espe.SecureFrameGallery.modules.steganography.dtos;

import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Resultado del análisis de esteganografía.
 * RF03 — Detección de LSB y análisis estructural.
 */
@Getter
@Builder
public class StegoAnalysisResultDto {

    /** Estado final determinado por el análisis. */
    private ImageStatus status;

    /**
     * Puntuación de entropía del plano LSB (0.0 a 1.0).
     * Valores > 0.75 indican posible información oculta.
     */
    private double lsbEntropyScore;

    /**
     * true si se encontraron datos después del marcador EOF de la imagen
     * (IEND para PNG, EOI para JPEG).
     */
    private boolean eofAnomalyFound;

    /**
     * Razones detalladas de la detección (para auditoría y log de cuarentena).
     * Ejemplo: ["Entropía LSB elevada (0.82)", "Datos extra tras marcador EOI"]
     */
    private List<String> detectionReasons;
}
