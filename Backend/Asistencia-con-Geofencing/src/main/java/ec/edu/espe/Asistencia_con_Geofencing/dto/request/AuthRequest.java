package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @NotBlank(message = "Provider is required (LOCAL, GOOGLE, FACEBOOK, EXTERNAL)")
    private String provider; // "LOCAL", "GOOGLE", "FACEBOOK", "EXTERNAL"
    private String email;
    private String password;
    private String token;
    
    // ===== CAMPOS PARA MANEJO DE NOTIFICACIONES PUSH =====
    /**
     * FCM Token del dispositivo (opcional en login)
     * Si se proporciona, se registra automáticamente el dispositivo
     * Solución profesional: Token sigue al usuario que inicia sesión
     */
    private String fcmToken;
    
    /**
     * Identificador único del dispositivo (opcional)
     * Ejemplo: "samsung_galaxy_s23_abc123", "iphone_14_xyz789"
     * Permite identificar el dispositivo específico
     */
    private String deviceIdentifier;

    public void validate() {
        String providerUpper = provider.toUpperCase();
        switch (providerUpper) {
            case "LOCAL":
                if (token == null || token.isEmpty()) {
                    throw new IllegalArgumentException("Token is required for LOCAL authentication (must be base64(email:password))");
                }
                break;

            case "GOOGLE":
            case "FACEBOOK":
            case "EXTERNAL":
                if (token == null || token.isEmpty()) {
                    throw new IllegalArgumentException("Token is required for " + providerUpper + " authentication");
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider +
                        ". Supported: LOCAL, GOOGLE, FACEBOOK, EXTERNAL");
        }
    }
}