package ec.edu.espe.Asistencia_con_Geofencing.dto.request;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserData {
    private String providerUserId;
    private String email;
    private String fullName;
}