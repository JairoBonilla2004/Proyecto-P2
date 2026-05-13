package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateLocationResponse {
    private Boolean withinCampus;
    private ZoneDistanceInfo nearestZone;
    private List<ZoneDistanceInfo> allZones;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneDistanceInfo {
        private java.util.UUID id;
        private String name;
        private Double distance;
        private Boolean withinZone;
    }
}