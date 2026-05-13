package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrTokenResponse {
    private UUID qrId;
    private String token;
    private UUID sessionId;
    private LocalDateTime expiresAt;
    private String qrCodeBase64;
}