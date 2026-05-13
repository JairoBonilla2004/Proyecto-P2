package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.UUID;

@Data
public class GenerateQrRequest {
    @NotNull(message = "El ID de sesión es requerido")
    private UUID sessionId;

    @Positive(message = "Los minutos deben ser positivos")
    private Integer expiresInMinutes = 10; // Default 10 minutos
}