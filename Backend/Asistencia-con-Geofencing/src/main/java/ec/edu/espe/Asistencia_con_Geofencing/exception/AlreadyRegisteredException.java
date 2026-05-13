package ec.edu.espe.Asistencia_con_Geofencing.exception;

import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class AlreadyRegisteredException extends RuntimeException {
    private final UUID attendanceId;
    private final LocalDateTime registeredAt;

    public AlreadyRegisteredException(String message, UUID attendanceId, LocalDateTime registeredAt) {
        super(message);
        this.attendanceId = attendanceId;
        this.registeredAt = registeredAt;
    }
}