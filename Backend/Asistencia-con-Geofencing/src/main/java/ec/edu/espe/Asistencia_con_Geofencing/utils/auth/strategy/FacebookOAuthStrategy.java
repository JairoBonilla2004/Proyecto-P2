package ec.edu.espe.Asistencia_con_Geofencing.utils.auth.strategy;


import ec.edu.espe.Asistencia_con_Geofencing.dto.OAuth.FacebookUserInfo;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.OAuthUserData;
import ec.edu.espe.Asistencia_con_Geofencing.exception.BadRequestException;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.OAuthProvider;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.ProviderType;
import ec.edu.espe.Asistencia_con_Geofencing.utils.auth.validators.FacebookTokenValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FacebookOAuthStrategy implements OAuthLoginStrategy {

    private final FacebookTokenValidator facebookTokenValidator;

    @Override
    public OAuthUserData validateAndExtractUserData(String accessToken) {
        FacebookUserInfo facebookInfo = facebookTokenValidator.validateToken(accessToken);

        if (facebookInfo.getEmail() == null || facebookInfo.getEmail().isEmpty()) {
            throw new BadRequestException("El email de Facebook es requerido");
        }

        return OAuthUserData.builder()
                .providerUserId(facebookInfo.getUserId())
                .email(facebookInfo.getEmail())
                .fullName(facebookInfo.getName())
                .build();
    }

    @Override
    public OAuthProvider getOAuthProvider() {
        return OAuthProvider.FACEBOOK;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.FACEBOOK;
    }
}