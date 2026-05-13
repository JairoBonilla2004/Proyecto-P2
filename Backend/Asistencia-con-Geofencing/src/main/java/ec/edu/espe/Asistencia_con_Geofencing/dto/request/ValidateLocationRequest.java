package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ValidateLocationRequest {
    @NotNull(message = "La latitud es requerida")
    private BigDecimal latitude;

    @NotNull(message = "La longitud es requerida")
    private BigDecimal longitude;
}