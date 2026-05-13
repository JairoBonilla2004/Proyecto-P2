package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RegisterSensorEventRequest {
    @NotBlank(message = "El tipo de sensor es requerido")
    private String type; // "COMPASS" o "PROXIMITY"

    @NotBlank(message = "El valor es requerido")
    private String value;

    @NotNull(message = "La hora del dispositivo es requerida")
    private LocalDateTime deviceTime;

    private UUID sessionId;
}