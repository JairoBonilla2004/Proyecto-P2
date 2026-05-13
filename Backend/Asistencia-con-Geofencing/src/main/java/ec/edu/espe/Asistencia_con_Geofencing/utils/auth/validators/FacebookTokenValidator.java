package ec.edu.espe.Asistencia_con_Geofencing.utils.auth.validators;

import ec.edu.espe.Asistencia_con_Geofencing.dto.OAuth.FacebookUserInfo;
import ec.edu.espe.Asistencia_con_Geofencing.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FacebookTokenValidator implements OAuthTokenValidator<FacebookUserInfo> {

    @Value("${oauth.facebook.app-id:}")
    private String facebookAppId;

    @Value("${oauth.facebook.app-secret:}")
    private String facebookAppSecret;


    private final WebClient.Builder webClientBuilder;

    @Override
    public FacebookUserInfo validateToken(String accessToken) {
        if (facebookAppId == null || facebookAppId.isEmpty()) {
            throw new UnauthorizedException("Facebook OAuth no está configurado en el servidor. " +
                    "Contacte al administrador del sistema.");
        }

        try {
            WebClient webClient = webClientBuilder.baseUrl("https://graph.facebook.com").build();

            Map<String, Object> debugResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/debug_token")
                            .queryParam("input_token", accessToken)
                            .queryParam("access_token", facebookAppId + "|" + facebookAppSecret)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (debugResponse == null || !isValidFacebookToken(debugResponse)) {
                throw new UnauthorizedException("Token de Facebook inválido");
            }

            Map<String, Object> userResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/me")
                            .queryParam("fields", "id,name,email")
                            .queryParam("access_token", accessToken)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (userResponse == null) {
                throw new UnauthorizedException("No se pudo obtener información del usuario de Facebook");
            }

            return FacebookUserInfo.builder()
                    .userId((String) userResponse.get("id"))
                    .email((String) userResponse.get("email"))
                    .name((String) userResponse.get("name"))
                    .build();

        } catch (Exception e) {
            log.error("Error validando token de Facebook", e);
            throw new UnauthorizedException("No se pudo validar el token de Facebook: " + e.getMessage());
        }
    }

    private boolean isValidFacebookToken(Map<String, Object> debugResponse) {
        Map<String, Object> data = (Map<String, Object>) debugResponse.get("data");
        if (data == null) {
            return false;
        }
        Boolean isValid = (Boolean) data.get("is_valid");
        return isValid != null && isValid;
    }
}