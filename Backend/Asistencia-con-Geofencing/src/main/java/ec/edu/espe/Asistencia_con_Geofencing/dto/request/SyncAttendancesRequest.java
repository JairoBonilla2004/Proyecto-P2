package ec.edu.espe.Asistencia_con_Geofencing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class SyncAttendancesRequest {
    @NotNull(message = "El ID de dispositivo es requerido")
    private UUID deviceId;

    @NotEmpty(message = "La lista de asistencias no puede estar vacía")
    private List<OfflineAttendanceData> attendances;

    @Data
    public static class OfflineAttendanceData {
        @NotNull
        private String tempId;

        @NotNull
        private String token;

        @NotNull
        private BigDecimal latitude;

        @NotNull
        private BigDecimal longitude;

        @NotNull
        private LocalDateTime deviceTime;

        @Valid
        private List<SensorDataDTO> sensorData = new ArrayList<>();
    }
}