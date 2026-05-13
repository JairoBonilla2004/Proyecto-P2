package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateFcmTokenRequest {
    @NotBlank(message = "El token FCM es requerido")
    private String fcmToken;
}