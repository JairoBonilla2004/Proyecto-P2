package ec.edu.espe.Asistencia_con_Geofencing.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "El nombre completo es requerido")
    private String fullName;
}
