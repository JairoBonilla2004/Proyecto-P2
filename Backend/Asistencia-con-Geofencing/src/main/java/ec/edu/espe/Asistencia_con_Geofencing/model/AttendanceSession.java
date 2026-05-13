package ec.edu.espe.Asistencia_con_Geofencing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "attendance_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "name", nullable = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geofence_id")
    private GeofenceZone geofence;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QrToken> qrTokens = new HashSet<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private Set<Attendance> attendances = new HashSet<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private Set<SensorEvent> sensorEvents = new HashSet<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private Set<ReportRequest> reportRequests = new HashSet<>();

}
