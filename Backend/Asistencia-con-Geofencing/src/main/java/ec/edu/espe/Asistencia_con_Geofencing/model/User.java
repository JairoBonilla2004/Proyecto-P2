package ec.edu.espe.Asistencia_con_Geofencing.model;

import ec.edu.espe.Asistencia_con_Geofencing.model.enums.ProviderType;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.RoleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ProviderType provider = ProviderType.LOCAL;

    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OAuthAccount> oauthAccounts = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Device> devices = new HashSet<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
    private Set<AttendanceSession> teacherSessions = new HashSet<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private Set<Attendance> attendances = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<SensorEvent> sensorEvents = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Notification> notifications = new HashSet<>();

    @OneToMany(mappedBy = "requestedBy", cascade = CascadeType.ALL)
    private Set<ReportRequest> reportRequests = new HashSet<>();

    public boolean hasRole(RoleType roleType) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleType.name()));
    }


}
