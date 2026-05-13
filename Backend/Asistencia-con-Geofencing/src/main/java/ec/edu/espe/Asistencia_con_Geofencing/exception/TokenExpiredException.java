package ec.edu.espe.Asistencia_con_Geofencing.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}