package ec.edu.espe.Asistencia_con_Geofencing.service.geofence;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.CreateGeofenceZoneRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.ValidateLocationRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.GeofenceZoneResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ValidateLocationResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface GeofenceService {

    List<GeofenceZoneResponse> getAllZones();
    GeofenceZoneResponse createZone(CreateGeofenceZoneRequest request);
    ValidateLocationResponse validateLocation(ValidateLocationRequest request);
    double calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2);
    void deleteZone(UUID zoneId);
}
