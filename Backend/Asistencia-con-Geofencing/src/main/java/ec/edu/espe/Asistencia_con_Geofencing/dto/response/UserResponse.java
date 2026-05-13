package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import ec.edu.espe.Asistencia_con_Geofencing.model.enums.ProviderType;
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
public class UserResponse {

    private UUID id;
    private String email;
    private String fullName;
    private ProviderType provider;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private List<String> roles;
}
