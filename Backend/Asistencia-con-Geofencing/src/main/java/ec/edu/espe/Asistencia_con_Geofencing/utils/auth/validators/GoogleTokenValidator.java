package ec.edu.espe.Asistencia_con_Geofencing.utils.auth.validators;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import ec.edu.espe.Asistencia_con_Geofencing.dto.OAuth.GoogleUserInfo;
import ec.edu.espe.Asistencia_con_Geofencing.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component //component se usa cuando es una clase que no pertenece a una capa especifica como servicio o repositorio solo es un componente reutilizable y queremos que spring la maneje como un bean para la inyeccion de dependencias
public class GoogleTokenValidator implements OAuthTokenValidator<GoogleUserInfo>{

    @Value("${oauth.google.client-id:}")
    private String googleClientId;

    @Override
    public GoogleUserInfo validateToken(String idToken) {
        if (googleClientId == null || googleClientId.isEmpty()) {
            throw new UnauthorizedException("Google OAuth no está configurado en el servidor. " +
                    "Contacte al administrador del sistema.");
        }
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new UnauthorizedException("Token de Google inválido");
            }

            GoogleIdToken.Payload payload = token.getPayload();

            return GoogleUserInfo.builder()
                    .userId(payload.getSubject())
                    .email(payload.getEmail())
                    .emailVerified(payload.getEmailVerified())
                    .name((String) payload.get("name"))
                    .pictureUrl((String) payload.get("picture"))
                    .build();

        } catch (Exception e) {
            log.error("Error validando token de Google", e);
            throw new UnauthorizedException("No se pudo validar el token de Google: " + e.getMessage());
        }
    }
}
