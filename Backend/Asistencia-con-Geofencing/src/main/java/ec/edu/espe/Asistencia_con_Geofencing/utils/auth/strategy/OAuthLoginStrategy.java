package ec.edu.espe.Asistencia_con_Geofencing.utils.auth.strategy;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.OAuthUserData;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.OAuthProvider;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.ProviderType;

public interface OAuthLoginStrategy {
    OAuthUserData validateAndExtractUserData(String token);
    OAuthProvider getOAuthProvider();
    ProviderType getProviderType();
}



