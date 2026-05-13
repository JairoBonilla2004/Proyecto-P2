package ec.edu.espe.Asistencia_con_Geofencing.utils.auth.strategy;

import ec.edu.espe.Asistencia_con_Geofencing.dto.OAuth.GoogleUserInfo;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.OAuthUserData;
import ec.edu.espe.Asistencia_con_Geofencing.exception.BadRequestException;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.OAuthProvider;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.ProviderType;
import ec.edu.espe.Asistencia_con_Geofencing.utils.auth.validators.GoogleTokenValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleOAuthStrategy implements OAuthLoginStrategy{

    private final GoogleTokenValidator googleTokenValidator;
    @Override
    public OAuthUserData validateAndExtractUserData(String idToken) {
        GoogleUserInfo googleInfo = googleTokenValidator.validateToken(idToken);

        if (googleInfo.getEmail() == null || googleInfo.getEmail().isEmpty()) {
            throw new BadRequestException("El email de Google es requerido");
        }

        return OAuthUserData.builder()
                .providerUserId(googleInfo.getUserId())
                .email(googleInfo.getEmail())
                .fullName(googleInfo.getName())
                .build();
    }

    @Override
    public OAuthProvider getOAuthProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.GOOGLE;
    }
}
