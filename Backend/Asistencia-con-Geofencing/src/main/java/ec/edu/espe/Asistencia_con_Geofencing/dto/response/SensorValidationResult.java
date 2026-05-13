package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorValidationResult {

    private boolean valid;
    private int trustScore; // 0-100
    private String validationLevel; // VALID, SUSPICIOUS, INVALID
    private String message;
    private SensorAnalysis compassAnalysis;
    private SensorAnalysis proximityAnalysis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorAnalysis {
        private int readingsCount;
        private boolean hasVariation;
        private double variationScore; // 0-100
        private String status; // OK, WARNING, ERROR
        private String details;
    }

    public static SensorValidationResult valid(int trustScore, String message) {
        return SensorValidationResult.builder()
                .valid(true)
                .trustScore(trustScore)
                .validationLevel("VALID")
                .message(message)
                .build();
    }

    public static SensorValidationResult suspicious(int trustScore, String message) {
        return SensorValidationResult.builder()
                .valid(true) // Permitimos pero marcamos como sospechoso
                .trustScore(trustScore)
                .validationLevel("SUSPICIOUS")
                .message(message)
                .build();
    }

    public static SensorValidationResult invalid(String message) {
        return SensorValidationResult.builder()
                .valid(false)
                .trustScore(0)
                .validationLevel("INVALID")
                .message(message)
                .build();
    }
}
