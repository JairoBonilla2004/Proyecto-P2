package ec.edu.espe.Asistencia_con_Geofencing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "attendances",
    uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "student_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "device_time", nullable = false)
    private LocalDateTime deviceTime;

    @CreationTimestamp
    @Column(name = "server_time", nullable = false, updatable = false)
    private LocalDateTime serverTime;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "within_geofence", nullable = false)
    private Boolean withinGeofence;

    @Column(name = "sensor_status", length = 100)
    private String sensorStatus;

    @Column(name = "trust_score")
    private Integer trustScore;

    @Column(name = "is_synced", nullable = false)
    private Boolean isSynced = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_device_id")
    private Device sourceDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sync_batch_id")
    private SyncBatch syncBatch;

    @OneToMany(mappedBy = "attendance", cascade = CascadeType.ALL)
    private Set<SensorEvent> sensorEvents = new HashSet<>();
}
