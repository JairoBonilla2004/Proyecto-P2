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
public class LogoutRequest {

    // Opcional: si no se proporciona, se desactivan todos los dispositivos del usuario
    private String deviceIdentifier;

    private String fcmToken;
}
