package ec.edu.espe.Asistencia_con_Geofencing.exception;

import lombok.Getter;

@Getter
public class InvalidSensorDataException extends RuntimeException {
    private final int trustScore;

    public InvalidSensorDataException(String message, int trustScore) {
        super(message);
        this.trustScore = trustScore;
    }
}
