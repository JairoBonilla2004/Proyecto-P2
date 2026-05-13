package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ValidateQrRequest {
    @NotBlank(message = "El token es requerido")
    private String token;

    @NotNull(message = "La latitud es requerida")
    private BigDecimal latitude;

    @NotNull(message = "La longitud es requerida")
    private BigDecimal longitude;

    @NotNull(message = "El ID del dispositivo es requerido")
    private UUID deviceId;

    @NotNull(message = "La hora del dispositivo es requerida")
    private LocalDateTime deviceTime;

    @Valid
    private List<SensorDataDTO> sensorData = new ArrayList<>();
}