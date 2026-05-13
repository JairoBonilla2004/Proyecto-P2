package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID userId;
    private String email;
    private String fullName;
    private String provider;
    private List<String> roles;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private List<OAuthAccountInfo> oauthAccounts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OAuthAccountInfo {
        private String provider;
        private LocalDateTime linkedAt;
    }
}