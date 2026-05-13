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
public class SessionWithDistanceResponse {
    private UUID sessionId;
    private String name;
    private String teacherName;
    private String zoneName;
    private Double zoneLatitude;
    private Double zoneLongitude;
    private Integer radiusMeters;
    private Double distanceInMeters;
    private Boolean withinZone;
    private String qrToken;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean active;
}
