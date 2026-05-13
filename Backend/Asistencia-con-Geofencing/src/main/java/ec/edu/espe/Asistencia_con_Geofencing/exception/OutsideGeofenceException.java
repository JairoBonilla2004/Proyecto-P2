package ec.edu.espe.Asistencia_con_Geofencing.exception;

import lombok.Getter;

@Getter
public class OutsideGeofenceException extends RuntimeException {
    private final String requiredZone;
    private final Double distance;
    private final Integer maxRadius;

    public OutsideGeofenceException(String message, String requiredZone, Double distance, Integer maxRadius) {
        super(message);
        this.requiredZone = requiredZone;
        this.distance = distance;
        this.maxRadius = maxRadius;
    }
}