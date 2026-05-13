package ec.edu.espe.Asistencia_con_Geofencing.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private Overview overview;
    private List<RecentAttendance> recentAttendances;
    private SyncStatus syncStatus;
    private NotificationSummary notifications;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {
        private Long totalSessions;
        private Long attendedSessions;
        private Double attendanceRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentAttendance {
        private UUID sessionId;
        private String teacherName;
        private String zoneName;
        private LocalDate date;
        private LocalTime time;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncStatus {
        private Integer pendingSync;
        private LocalDateTime lastSyncAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationSummary {
        private Integer unreadCount;
    }
}