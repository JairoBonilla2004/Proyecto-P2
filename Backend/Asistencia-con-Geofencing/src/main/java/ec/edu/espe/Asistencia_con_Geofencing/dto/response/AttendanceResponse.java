package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private UUID attendanceId;
    private UUID sessionId;
    private UUID studentId;
    private String studentName;
    private LocalDateTime deviceTime;
    private LocalDateTime serverTime;
    private Boolean withinGeofence;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String sensorStatus;
    private Integer trustScore;
    private Boolean isSynced;
    private String syncDelay;
    private String note;
}