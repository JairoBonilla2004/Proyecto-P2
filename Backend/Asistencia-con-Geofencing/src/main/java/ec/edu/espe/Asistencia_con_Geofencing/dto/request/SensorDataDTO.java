package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import ec.edu.espe.Asistencia_con_Geofencing.model.enums.SensorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataDTO {

    @NotNull(message = "El tipo de sensor es obligatorio")
    private SensorType type;

    @NotBlank(message = "El valor del sensor es obligatorio")
    private String value;

    @NotNull(message = "El timestamp del dispositivo es obligatorio")
    private LocalDateTime deviceTime;
}
