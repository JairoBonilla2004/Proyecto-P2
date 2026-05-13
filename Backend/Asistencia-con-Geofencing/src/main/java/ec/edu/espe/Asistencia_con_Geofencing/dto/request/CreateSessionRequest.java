package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateSessionRequest {
    @NotNull(message = "El ID de geofence es requerido")
    private UUID geofenceId;

    @NotNull(message = "El nombre es requerido")
    private String name;

    @NotNull(message = "La hora de inicio es requerida")
    private LocalDateTime startTime;
}