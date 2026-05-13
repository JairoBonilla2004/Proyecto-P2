package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import ec.edu.espe.Asistencia_con_Geofencing.model.AttendanceSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public class SessionStatisticsResponse {

    private UUID sessionId;
    private String teacherName;
    private String zoneName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String duration;
    private int totalAttendances;
    private long onTimeRegistrations;
    private long lateRegistrations;
    private double averageSyncDelay;

    public SessionStatisticsResponse(
            AttendanceSession session,
            Duration duration,
            int totalAttendances,
            long onTime,
            long late,
            double avgDelay) {

        this.sessionId = session.getId();
        this.teacherName = session.getTeacher().getFullName();
        this.zoneName = session.getGeofence().getName();
        this.startTime = session.getStartTime();
        this.endTime = session.getEndTime();
        this.duration = duration != null
                ? duration.toHours() + "h " + (duration.toMinutes() % 60) + "m"
                : "En curso";
        this.totalAttendances = totalAttendances;
        this.onTimeRegistrations = onTime;
        this.lateRegistrations = late;
        this.averageSyncDelay = Math.round(avgDelay * 100.0) / 100.0;
    }
}
