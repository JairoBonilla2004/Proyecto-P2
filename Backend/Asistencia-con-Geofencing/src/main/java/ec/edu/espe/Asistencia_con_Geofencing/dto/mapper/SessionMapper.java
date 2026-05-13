package ec.edu.espe.Asistencia_con_Geofencing.dto.mapper;

import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SessionResponse;
import ec.edu.espe.Asistencia_con_Geofencing.model.AttendanceSession;

public class SessionMapper {

    public static SessionResponse mapToResponse(AttendanceSession session) {
        SessionResponse.SessionResponseBuilder builder = SessionResponse.builder()
                .sessionId(session.getId())
                .name(session.getName())
                .teacherId(session.getTeacher().getId())
                .teacherName(session.getTeacher().getFullName())
                .geofenceId(session.getGeofence().getId())
                .zoneName(session.getGeofence().getName())
                .zoneLatitude(session.getGeofence().getLatitude().doubleValue())
                .zoneLongitude(session.getGeofence().getLongitude().doubleValue())
                .radiusMeters(session.getGeofence().getRadiusMeters().doubleValue())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .active(session.getActive());

        return builder.build();
    }
}
