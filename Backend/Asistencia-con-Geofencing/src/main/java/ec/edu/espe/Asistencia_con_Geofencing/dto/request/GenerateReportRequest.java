package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class GenerateReportRequest {
    @NotBlank(message = "El tipo de reporte es requerido")
    private String reportType;

    private LocalDate startDate;
    private LocalDate endDate;
    private UUID sessionId;
}