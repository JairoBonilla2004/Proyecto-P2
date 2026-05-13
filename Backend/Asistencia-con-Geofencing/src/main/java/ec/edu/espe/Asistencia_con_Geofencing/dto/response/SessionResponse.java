package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private UUID sessionId;
    private String name;
    private UUID teacherId;
    private String teacherName;
    private UUID geofenceId;
    private String zoneName;
    private Double zoneLatitude;
    private Double zoneLongitude;
    private Double radiusMeters;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean active;
    private Boolean hasActiveQR;
    private Integer totalAttendances;
}