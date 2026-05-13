package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardResponse {

    private Integer totalSessions;
    private Integer activeSessions;
    private Integer totalStudentsEnrolled;
    private Double averageAttendanceRate;
    private List<SessionSummary> recentSessions;
    
    private Map<String, Integer> attendanceByMonth;
    private Map<String, Double> attendanceRateBySession;
    
    private Integer totalAttendances;
    private LocalDate lastSessionDate;
    private String mostActiveSession;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionSummary {
        private String sessionId;
        private String sessionName;
        private String zoneName;
        private LocalDate date;
        private Integer totalAttendances;
        private Double attendanceRate;
        private Boolean isActive;
    }
}
