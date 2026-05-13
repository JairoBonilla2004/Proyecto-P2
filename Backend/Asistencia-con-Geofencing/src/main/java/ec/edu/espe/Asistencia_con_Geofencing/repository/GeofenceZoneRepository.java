package ec.edu.espe.Asistencia_con_Geofencing.repository;


import ec.edu.espe.Asistencia_con_Geofencing.model.GeofenceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeofenceZoneRepository extends JpaRepository<GeofenceZone, UUID> {

    @Query(value = """
        SELECT 
            gz.id,
            gz.name,
            gz.radius_meters,
            (6371000 * acos(
                cos(radians(:latitude)) * cos(radians(gz.latitude)) * 
                cos(radians(gz.longitude) - radians(:longitude)) + 
                sin(radians(:latitude)) * sin(radians(gz.latitude))
            )) as distance_meters
        FROM geofence_zones gz
        ORDER BY distance_meters
        """, nativeQuery = true)
    List<Object[]> findAllWithDistance(BigDecimal latitude, BigDecimal longitude); // se pone la cantidad 637000 es el radio de la tierra en metros
}