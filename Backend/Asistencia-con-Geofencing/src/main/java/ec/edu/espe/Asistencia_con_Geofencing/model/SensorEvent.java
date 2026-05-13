package ec.edu.espe.Asistencia_con_Geofencing.model;

import ec.edu.espe.Asistencia_con_Geofencing.model.enums.SensorType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sensor_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SensorType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "device_time", nullable = false)
    private LocalDateTime deviceTime;

    @CreationTimestamp
    @Column(name = "server_time", nullable = false, updatable = false)
    private LocalDateTime serverTime;
}
