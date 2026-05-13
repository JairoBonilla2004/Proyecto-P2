package ec.edu.espe.Asistencia_con_Geofencing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "geofence_zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceZone {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "radius_meters", nullable = false)
    private Integer radiusMeters;

    @OneToMany(mappedBy = "geofence", cascade = CascadeType.ALL)
    private Set<AttendanceSession> sessions = new HashSet<>();
}
