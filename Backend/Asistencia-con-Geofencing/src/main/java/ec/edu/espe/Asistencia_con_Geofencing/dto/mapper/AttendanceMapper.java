package ec.edu.espe.Asistencia_con_Geofencing.dto.mapper;

import ec.edu.espe.Asistencia_con_Geofencing.dto.response.AttendanceResponse;
import ec.edu.espe.Asistencia_con_Geofencing.model.Attendance;

import java.time.Duration;

public class AttendanceMapper {

    public static AttendanceResponse mapToResponse(Attendance attendance) {
        Duration syncDelay = null;
        String syncDelayStr = "N/A";
        
        if (attendance.getDeviceTime() != null && attendance.getServerTime() != null) {
            syncDelay = Duration.between(attendance.getDeviceTime(), attendance.getServerTime());
            syncDelayStr = formatDuration(syncDelay);
        }

        return AttendanceResponse.builder()
                .attendanceId(attendance.getId())
                .sessionId(attendance.getSession().getId())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent().getFullName())
                .deviceTime(attendance.getDeviceTime())
                .serverTime(attendance.getServerTime())
                .withinGeofence(attendance.getWithinGeofence())
                .latitude(attendance.getLatitude())
                .longitude(attendance.getLongitude())
                .sensorStatus(attendance.getSensorStatus())
                .trustScore(attendance.getTrustScore())
                .isSynced(attendance.getIsSynced())
                .syncDelay(syncDelayStr)
                .build();
    }

    private static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + " minutes " + remainingSeconds + " seconds";
        } else {
            long hours = seconds / 3600;
            long remainingMinutes = (seconds % 3600) / 60;
            return hours + " hours " + remainingMinutes + " minutes";
        }
    }
}
