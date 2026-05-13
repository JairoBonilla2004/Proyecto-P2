package ec.edu.espe.Asistencia_con_Geofencing.exception;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class SessionInactiveException extends RuntimeException {
    private final LocalDateTime endTime;

    public SessionInactiveException(String message, LocalDateTime endTime) {
        super(message);
        this.endTime = endTime;
    }
}