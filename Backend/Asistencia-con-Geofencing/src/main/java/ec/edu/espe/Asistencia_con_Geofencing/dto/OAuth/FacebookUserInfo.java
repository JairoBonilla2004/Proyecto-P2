package ec.edu.espe.Asistencia_con_Geofencing.dto.OAuth;

@lombok.Builder
@lombok.Data
public class FacebookUserInfo {
    private String userId;
    private String email;
    private String name;
}