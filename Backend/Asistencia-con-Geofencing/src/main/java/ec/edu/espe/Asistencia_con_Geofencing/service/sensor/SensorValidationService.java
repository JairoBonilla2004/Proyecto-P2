package ec.edu.espe.Asistencia_con_Geofencing.service.sensor;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.SensorDataDTO;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SensorValidationResult;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.SensorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SensorValidationService {

    private static final int MIN_READINGS = 2;
    private static final int IDEAL_READINGS = 5;
    private static final double MIN_COMPASS_VARIATION = 5.0; // grados
    private static final long MAX_TIME_SPAN_SECONDS = 10;

    /**
     * Valida los datos de sensores para una asistencia
     */
    public SensorValidationResult validateSensorData(List<SensorDataDTO> sensorData) {
        if (sensorData == null || sensorData.isEmpty()) {
            return SensorValidationResult.suspicious(60,
                    "No se recibieron datos de sensores. Validación solo con GPS y QR.");
        }

        // Agrupar por tipo de sensor
        Map<SensorType, List<SensorDataDTO>> groupedBySensor = sensorData.stream()
                .collect(Collectors.groupingBy(SensorDataDTO::getType));

        List<SensorDataDTO> compassData = groupedBySensor.getOrDefault(SensorType.COMPASS, List.of());
        List<SensorDataDTO> proximityData = groupedBySensor.getOrDefault(SensorType.PROXIMITY, List.of());

        // Validar timestamps
        if (!validateTimestamps(sensorData)) {
            return SensorValidationResult.invalid(
                    "Los timestamps de los sensores son inválidos o están fuera del rango permitido.");
        }

        // Analizar cada tipo de sensor
        SensorValidationResult.SensorAnalysis compassAnalysis = analyzeCompassData(compassData);
        SensorValidationResult.SensorAnalysis proximityAnalysis = analyzeProximityData(proximityData);

        // Calcular score de confianza
        int trustScore = calculateTrustScore(compassAnalysis, proximityAnalysis, sensorData.size());

        // Determinar resultado
        String validationLevel;
        String message;
        boolean isValid;

        if (trustScore >= 70) {
            validationLevel = "VALID";
            message = "Datos de sensores válidos. Alta confiabilidad.";
            isValid = true;
        } else if (trustScore >= 50) {
            validationLevel = "SUSPICIOUS";
            message = "Datos de sensores sospechosos. Revisar manualmente.";
            isValid = true; // Permitimos pero marcamos para revisión
        } else {
            validationLevel = "INVALID";
            message = "Datos de sensores inválidos. Posible intento de fraude.";
            isValid = false;
        }

        return SensorValidationResult.builder()
                .valid(isValid)
                .trustScore(trustScore)
                .validationLevel(validationLevel)
                .message(message)
                .compassAnalysis(compassAnalysis)
                .proximityAnalysis(proximityAnalysis)
                .build();
    }

    /**
     * Valida que los timestamps estén en un rango razonable
     */
    private boolean validateTimestamps(List<SensorDataDTO> sensorData) {
        if (sensorData.size() < 2) return true;

        var times = sensorData.stream()
                .map(SensorDataDTO::getDeviceTime)
                .sorted()
                .toList();

        Duration span = Duration.between(times.get(0), times.get(times.size() - 1));
        return span.getSeconds() <= MAX_TIME_SPAN_SECONDS;
    }

    /**
     * Analiza datos del sensor de brújula
     */
    private SensorValidationResult.SensorAnalysis analyzeCompassData(List<SensorDataDTO> compassData) {
        if (compassData.isEmpty()) {
            return SensorValidationResult.SensorAnalysis.builder()
                    .readingsCount(0)
                    .hasVariation(false)
                    .variationScore(0)
                    .status("WARNING")
                    .details("No se recibieron datos de brújula")
                    .build();
        }

        if (compassData.size() < MIN_READINGS) {
            return SensorValidationResult.SensorAnalysis.builder()
                    .readingsCount(compassData.size())
                    .hasVariation(false)
                    .variationScore(20)
                    .status("WARNING")
                    .details("Lecturas insuficientes de brújula")
                    .build();
        }

        // Calcular variación en los valores
        double variation = calculateCompassVariation(compassData);
        boolean hasVariation = variation >= MIN_COMPASS_VARIATION;

        String status;
        String details;
        double score;

        if (variation >= MIN_COMPASS_VARIATION * 2) {
            status = "OK";
            details = String.format("Variación natural detectada (%.1f°)", variation);
            score = 100;
        } else if (variation >= MIN_COMPASS_VARIATION) {
            status = "OK";
            details = String.format("Variación mínima aceptable (%.1f°)", variation);
            score = 70;
        } else {
            status = "ERROR";
            details = String.format("Sin variación significativa (%.1f°). Posible screenshot.", variation);
            score = 10;
        }

        return SensorValidationResult.SensorAnalysis.builder()
                .readingsCount(compassData.size())
                .hasVariation(hasVariation)
                .variationScore(score)
                .status(status)
                .details(details)
                .build();
    }

    /**
     * Calcula la variación en las lecturas de brújula
     */
    private double calculateCompassVariation(List<SensorDataDTO> compassData) {
        try {
            List<Double> azimuthValues = compassData.stream()
                    .map(data -> {
                        String value = data.getValue();
                        // Parse JSON simple: {"azimuth": "180.5", "pitch": "10.2"} o {"azimuth": 180.5}
                        String azimuthStr = value.split("\"azimuth\":")[1]
                                .split(",")[0]
                                .trim()
                                .replaceAll("\"", ""); // Remover comillas si existen
                        return Double.parseDouble(azimuthStr);
                    })
                    .toList();

            if (azimuthValues.size() < 2) return 0.0;

            double max = azimuthValues.stream().max(Double::compare).orElse(0.0);
            double min = azimuthValues.stream().min(Double::compare).orElse(0.0);

            return max - min;
        } catch (Exception e) {
            log.warn("Error calculando variación de brújula: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Analiza datos del sensor de proximidad
     */
    private SensorValidationResult.SensorAnalysis analyzeProximityData(List<SensorDataDTO> proximityData) {
        if (proximityData.isEmpty()) {
            return SensorValidationResult.SensorAnalysis.builder()
                    .readingsCount(0)
                    .hasVariation(false)
                    .variationScore(0)
                    .status("WARNING")
                    .details("No se recibieron datos de proximidad")
                    .build();
        }

        // Contar cuántas veces el sensor detectó proximidad (near = true)
        long nearCount = proximityData.stream()
                .filter(data -> data.getValue().contains("\"near\":true") || 
                               data.getValue().contains("\"near\": true"))
                .count();

        double nearPercentage = (double) nearCount / proximityData.size() * 100;

        String status;
        String details;
        double score;

        if (nearPercentage >= 50) {
            status = "OK";
            details = String.format("Proximidad detectada en %.0f%% de lecturas", nearPercentage);
            score = 100;
        } else if (nearPercentage >= 20) {
            status = "WARNING";
            details = String.format("Proximidad ocasional (%.0f%%)", nearPercentage);
            score = 60;
        } else {
            status = "ERROR";
            details = String.format("Sin proximidad detectada (%.0f%%)", nearPercentage);
            score = 20;
        }

        return SensorValidationResult.SensorAnalysis.builder()
                .readingsCount(proximityData.size())
                .hasVariation(nearCount > 0)
                .variationScore(score)
                .status(status)
                .details(details)
                .build();
    }

    /**
     * Calcula el score de confianza general
     */
    private int calculateTrustScore(
            SensorValidationResult.SensorAnalysis compassAnalysis,
            SensorValidationResult.SensorAnalysis proximityAnalysis,
            int totalReadings) {

        // Peso: 40% brújula, 40% proximidad, 20% cantidad de lecturas
        double compassWeight = 0.40;
        double proximityWeight = 0.40;
        double readingsWeight = 0.20;

        double compassScore = compassAnalysis != null ? compassAnalysis.getVariationScore() : 0;
        double proximityScore = proximityAnalysis != null ? proximityAnalysis.getVariationScore() : 0;

        // Score por cantidad de lecturas
        double readingsScore = Math.min(100, (totalReadings / (double) IDEAL_READINGS) * 100);

        return (int) Math.round(
                compassScore * compassWeight +
                proximityScore * proximityWeight +
                readingsScore * readingsWeight
        );
    }
}
