package ec.edu.espe.Asistencia_con_Geofencing.utils.auth.validators;

public interface OAuthTokenValidator<T> {

    T validateToken(String token);
}
