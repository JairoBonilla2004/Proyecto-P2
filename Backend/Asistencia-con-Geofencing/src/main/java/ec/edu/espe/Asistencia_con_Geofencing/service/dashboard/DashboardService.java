package ec.edu.espe.Asistencia_con_Geofencing.service.dashboard;

import ec.edu.espe.Asistencia_con_Geofencing.dto.response.DashboardResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SessionStatisticsResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.TeacherDashboardResponse;
import java.util.UUID;

public interface DashboardService {

    DashboardResponse getStudentDashboard(UUID studentId);
    SessionStatisticsResponse getSessionStatistics(UUID sessionId, UUID teacherId);
    TeacherDashboardResponse getTeacherDashboard(UUID teacherId);

}
