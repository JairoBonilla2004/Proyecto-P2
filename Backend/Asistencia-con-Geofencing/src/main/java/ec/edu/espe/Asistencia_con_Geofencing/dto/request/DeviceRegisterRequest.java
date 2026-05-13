package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegisterRequest {
    @NotBlank(message = "El identificador del dispositivo es requerido")
    private String deviceIdentifier;

    // Opcional: se detecta automáticamente desde deviceIdentifier si no se proporciona
    private String platform; // "ANDROID" o "IOS"

    private String fcmToken;
}
