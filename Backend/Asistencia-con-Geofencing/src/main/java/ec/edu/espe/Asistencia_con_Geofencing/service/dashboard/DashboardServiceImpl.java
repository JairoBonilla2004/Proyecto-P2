package ec.edu.espe.Asistencia_con_Geofencing.service.dashboard;

import ec.edu.espe.Asistencia_con_Geofencing.dto.response.DashboardResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SessionStatisticsResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.TeacherDashboardResponse;
import ec.edu.espe.Asistencia_con_Geofencing.exception.ResourceNotFoundException;
import ec.edu.espe.Asistencia_con_Geofencing.exception.UnauthorizedException;
import ec.edu.espe.Asistencia_con_Geofencing.model.Attendance;
import ec.edu.espe.Asistencia_con_Geofencing.model.AttendanceSession;
import ec.edu.espe.Asistencia_con_Geofencing.repository.AttendanceRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.AttendanceSessionRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AttendanceRepository attendanceRepository;
    private final NotificationRepository notificationRepository;
    private final AttendanceSessionRepository sessionRepository;
    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getStudentDashboard(UUID studentId) {
        Long attendedSessions = attendanceRepository.countAttendedSessionsByStudentId(studentId);
        long totalSessions = sessionRepository.count();
        Double attendanceRate = totalSessions > 0
                ? Math.round((attendedSessions.doubleValue() / totalSessions) * 10000.0) / 100.0
                : 0.0;

        DashboardResponse.Overview overview = DashboardResponse.Overview.builder()
                .totalSessions(totalSessions)
                .attendedSessions(attendedSessions)
                .attendanceRate(attendanceRate)
                .build();

        List<Attendance> recentAttendances = attendanceRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDateTime.now().minusMonths(1),
                LocalDateTime.now(),
                PageRequest.of(0, 5)
        ).getContent();

        List<DashboardResponse.RecentAttendance> recentList = recentAttendances.stream()
                .map(a -> DashboardResponse.RecentAttendance.builder()
                        .sessionId(a.getSession().getId())
                        .teacherName(a.getSession().getTeacher().getFullName())
                        .zoneName(a.getSession().getGeofence().getName())
                        .date(a.getDeviceTime().toLocalDate())
                        .time(a.getDeviceTime().toLocalTime())
                        .build())
                .collect(Collectors.toList());

        List<Attendance> pendingSync = attendanceRepository.findByStudentIdAndIsSyncedFalse(studentId);
        LocalDateTime lastSyncAt = recentAttendances.stream()
                .filter(Attendance::getIsSynced)
                .map(Attendance::getServerTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        DashboardResponse.SyncStatus syncStatus = DashboardResponse.SyncStatus.builder()
                .pendingSync(pendingSync.size())
                .lastSyncAt(lastSyncAt)
                .build();
        int unreadCount = notificationRepository.findUnreadByUserId(studentId).size();

        DashboardResponse.NotificationSummary notificationSummary = DashboardResponse.NotificationSummary.builder()
                .unreadCount(unreadCount)
                .build();

        return DashboardResponse.builder()
                .overview(overview)
                .recentAttendances(recentList)
                .syncStatus(syncStatus)
                .notifications(notificationSummary)
                .build();
    }

    @Override
    public SessionStatisticsResponse getSessionStatistics(UUID sessionId, UUID teacherId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada"));

        if (!session.getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("No tienes permiso para ver esta sesión");
        }

        List<Attendance> attendances =
                attendanceRepository.findBySessionId(sessionId);

        return buildStatistics(session, attendances);
    }

    private SessionStatisticsResponse buildStatistics(
            AttendanceSession session,
            List<Attendance> attendances) {

        Duration duration = session.getEndTime() != null
                ? Duration.between(session.getStartTime(), session.getEndTime())
                : null;

        long onTime = attendances.stream()
                .filter(a -> a.getDeviceTime()
                        .isBefore(session.getStartTime().plusMinutes(5)))
                .count();

        long late = attendances.size() - onTime;

        double avgDelay = attendances.stream()
                .mapToLong(a -> Duration
                        .between(a.getDeviceTime(), a.getServerTime())
                        .getSeconds())
                .average()
                .orElse(0);

        return new SessionStatisticsResponse(
                session,
                duration,
                attendances.size(),
                onTime,
                late,
                avgDelay
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherDashboardResponse getTeacherDashboard(UUID teacherId) {
        List<AttendanceSession> allSessions = sessionRepository.findByTeacherId(teacherId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        
        int totalSessions = allSessions.size();
        long activeSessions = allSessions.stream()
                .filter(AttendanceSession::getActive)
                .count();

        Set<UUID> uniqueStudents = new HashSet<>();
        List<Attendance> allAttendances = new ArrayList<>();
        
        for (AttendanceSession session : allSessions) {
            List<Attendance> sessionAttendances = attendanceRepository.findBySessionId(session.getId());
            allAttendances.addAll(sessionAttendances);
            sessionAttendances.forEach(a -> uniqueStudents.add(a.getStudent().getId()));
        }

        int totalStudentsEnrolled = uniqueStudents.size();
        double averageAttendanceRate = 0.0;
        if (!allSessions.isEmpty()) {
            List<Double> rates = allSessions.stream()
                    .map(session -> {
                        List<Attendance> sessionAttendances = attendanceRepository.findBySessionId(session.getId());
                        return sessionAttendances.size();
                    })
                    .map(count -> count.doubleValue())
                    .collect(Collectors.toList());
            
            double totalAttendances = rates.stream().mapToDouble(Double::doubleValue).sum();
            double maxPossible = totalStudentsEnrolled * totalSessions;
            averageAttendanceRate = maxPossible > 0 ? Math.round((totalAttendances / maxPossible) * 10000.0) / 100.0 : 0.0;
        }

        List<TeacherDashboardResponse.SessionSummary> recentSessions = allSessions.stream()
                .sorted((s1, s2) -> s2.getStartTime().compareTo(s1.getStartTime()))
                .limit(5)
                .map(session -> {
                    List<Attendance> sessionAttendances = attendanceRepository.findBySessionId(session.getId());
                    double rate = uniqueStudents.size() > 0 
                            ? Math.round((sessionAttendances.size() * 100.0 / uniqueStudents.size()) * 100.0) / 100.0 
                            : 0.0;
                    
                    return TeacherDashboardResponse.SessionSummary.builder()
                            .sessionId(session.getId().toString())
                            .sessionName(session.getName())
                            .zoneName(session.getGeofence().getName())
                            .date(session.getStartTime().toLocalDate())
                            .totalAttendances(sessionAttendances.size())
                            .attendanceRate(rate)
                            .isActive(session.getActive())
                            .build();
                })
                .toList();

        Map<String, Integer> attendanceByMonth = allAttendances.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getDeviceTime().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.summingInt(a -> 1)
                ));

        Map<String, Double> attendanceRateBySession = allSessions.stream()
                .collect(Collectors.toMap(
                        AttendanceSession::getName,
                        session -> {
                            List<Attendance> sessionAttendances = attendanceRepository.findBySessionId(session.getId());
                            return uniqueStudents.size() > 0 
                                    ? Math.round((sessionAttendances.size() * 100.0 / uniqueStudents.size()) * 100.0) / 100.0
                                    : 0.0;
                        },
                        (rate1, rate2) -> Math.round(((rate1 + rate2) / 2) * 100.0) / 100.0
                ));

        int totalAttendances = allAttendances.size();
        
        LocalDate lastSessionDate = allSessions.stream()
                .map(AttendanceSession::getStartTime)
                .max(LocalDateTime::compareTo)
                .map(LocalDateTime::toLocalDate)
                .orElse(null);

        String mostActiveSession = allSessions.stream()
                .max((s1, s2) -> {
                    int count1 = attendanceRepository.findBySessionId(s1.getId()).size();
                    int count2 = attendanceRepository.findBySessionId(s2.getId()).size();
                    return Integer.compare(count1, count2);
                })
                .map(AttendanceSession::getName)
                .orElse("N/A");

        return TeacherDashboardResponse.builder()
                .totalSessions(totalSessions)
                .activeSessions((int) activeSessions)
                .totalStudentsEnrolled(totalStudentsEnrolled)
                .averageAttendanceRate(averageAttendanceRate)
                .recentSessions(recentSessions)
                .attendanceByMonth(attendanceByMonth)
                .attendanceRateBySession(attendanceRateBySession)
                .totalAttendances(totalAttendances)
                .lastSessionDate(lastSessionDate)
                .mostActiveSession(mostActiveSession)
                .build();
    }

}