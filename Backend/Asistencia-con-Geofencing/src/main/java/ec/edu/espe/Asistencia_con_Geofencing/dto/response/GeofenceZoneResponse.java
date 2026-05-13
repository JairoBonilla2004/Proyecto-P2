package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceZoneResponse {
    private UUID id;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer radiusMeters;
}
