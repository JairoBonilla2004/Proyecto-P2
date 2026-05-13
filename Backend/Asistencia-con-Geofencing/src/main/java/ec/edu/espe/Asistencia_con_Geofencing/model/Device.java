package ec.edu.espe.Asistencia_con_Geofencing.model;

import ec.edu.espe.Asistencia_con_Geofencing.model.enums.PlatformType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "devices",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_identifier"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_identifier", nullable = false, length = 255)
    private String deviceIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlatformType platform;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @UpdateTimestamp
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @OneToMany(mappedBy = "sourceDevice", cascade = CascadeType.ALL)
    private Set<Attendance> attendances = new HashSet<>();

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
    private Set<SyncBatch> syncBatches = new HashSet<>();
}
