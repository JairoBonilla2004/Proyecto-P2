package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateGeofenceZoneRequest {
    @NotBlank(message = "El nombre es requerido")
    private String name;

    @NotNull(message = "La latitud es requerida")
    private BigDecimal latitude;

    @NotNull(message = "La longitud es requerida")
    private BigDecimal longitude;

    @NotNull(message = "El radio es requerido")
    @Positive(message = "El radio debe ser positivo")
    private Integer radiusMeters;
}