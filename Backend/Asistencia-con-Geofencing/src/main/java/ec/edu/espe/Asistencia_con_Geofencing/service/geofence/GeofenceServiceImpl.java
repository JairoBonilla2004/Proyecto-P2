package ec.edu.espe.Asistencia_con_Geofencing.service.geofence;


import ec.edu.espe.Asistencia_con_Geofencing.dto.mapper.GeofenceMapper;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.CreateGeofenceZoneRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.ValidateLocationRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.GeofenceZoneResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ValidateLocationResponse;
import ec.edu.espe.Asistencia_con_Geofencing.model.GeofenceZone;
import ec.edu.espe.Asistencia_con_Geofencing.repository.GeofenceZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeofenceServiceImpl implements GeofenceService {

    private final GeofenceZoneRepository geofenceZoneRepository;
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    @Override
    @Transactional(readOnly = true)
    public List<GeofenceZoneResponse> getAllZones() {
        return geofenceZoneRepository.findAll().stream()
                .map(GeofenceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GeofenceZoneResponse createZone(CreateGeofenceZoneRequest request) {
        GeofenceZone zone = new GeofenceZone();
        zone.setName(request.getName());
        zone.setLatitude(request.getLatitude());
        zone.setLongitude(request.getLongitude());
        zone.setRadiusMeters(request.getRadiusMeters());
        zone = geofenceZoneRepository.save(zone);
        return GeofenceMapper.toResponse(zone);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateLocationResponse validateLocation(ValidateLocationRequest request) {
        List<Object[]> zonesWithDistance = geofenceZoneRepository
                .findAllWithDistance(request.getLatitude(), request.getLongitude());

        List<ValidateLocationResponse.ZoneDistanceInfo> allZones = zonesWithDistance.stream()
                .map(row -> {
                    java.util.UUID zoneId = java.util.UUID.fromString(row[0].toString());
                    String zoneName = row[1].toString();
                    Integer radiusMeters = ((Number) row[2]).intValue();
                    Double distance = ((Number) row[3]).doubleValue();
                    Boolean withinZone = distance <= radiusMeters;

                    return ValidateLocationResponse.ZoneDistanceInfo.builder()
                            .id(zoneId)
                            .name(zoneName)
                            .distance(distance)
                            .withinZone(withinZone)
                            .build();
                })
                .collect(Collectors.toList());

        ValidateLocationResponse.ZoneDistanceInfo nearest = allZones.isEmpty() ? null : allZones.get(0);
        Boolean withinCampus = nearest != null && nearest.getWithinZone();

        return ValidateLocationResponse.builder()
                .withinCampus(withinCampus)
                .nearestZone(nearest)
                .allZones(allZones)
                .build();
    }

    @Override
    public double calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double deltaLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double deltaLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    @Override
    public void deleteZone(UUID zoneId) {
        geofenceZoneRepository.findById( zoneId).orElseThrow(() ->
            new IllegalArgumentException("Zona de geocerca no encontrada con ID: " + zoneId));
        geofenceZoneRepository.deleteById(zoneId);
    }

}