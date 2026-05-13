package ec.edu.espe.Asistencia_con_Geofencing.dto.OAuth;

@lombok.Builder
@lombok.Data
public class GoogleUserInfo {
    private String userId;
    private String email;
    private Boolean emailVerified;
    private String name;
    private String pictureUrl;
}