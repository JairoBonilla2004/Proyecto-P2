package ec.edu.espe.Asistencia_con_Geofencing.dto.mapper;

import ec.edu.espe.Asistencia_con_Geofencing.dto.response.GeofenceZoneResponse;
import ec.edu.espe.Asistencia_con_Geofencing.model.GeofenceZone;
import org.springframework.stereotype.Component;

@Component
public class GeofenceMapper {

    public static GeofenceZoneResponse toResponse(GeofenceZone zone) {
        return GeofenceZoneResponse.builder()
                .id(zone.getId())
                .name(zone.getName())
                .latitude(zone.getLatitude())
                .longitude(zone.getLongitude())
                .radiusMeters(zone.getRadiusMeters())
                .build();
    }
}
