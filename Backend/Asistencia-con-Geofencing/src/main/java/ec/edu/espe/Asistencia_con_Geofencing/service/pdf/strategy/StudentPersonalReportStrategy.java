package ec.edu.espe.Asistencia_con_Geofencing.service.pdf.strategy;

import ec.edu.espe.Asistencia_con_Geofencing.exception.ResourceNotFoundException;
import ec.edu.espe.Asistencia_con_Geofencing.model.Attendance;
import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import ec.edu.espe.Asistencia_con_Geofencing.repository.AttendanceRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.AttendanceSessionRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.UserRepository;
import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfTableData;
import ec.edu.espe.Asistencia_con_Geofencing.utils.pdf.DateTimeFormatters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
@Slf4j
public class StudentPersonalReportStrategy extends AbstractPdfReportStrategy {
    
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final UserRepository userRepository;
    
    public StudentPersonalReportStrategy(
            DateTimeFormatters dateTimeFormatters,
            AttendanceRepository attendanceRepository,
            AttendanceSessionRepository sessionRepository,
            UserRepository userRepository) {
        super(dateTimeFormatters);
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }
    
    @Override
    public String getReportType() {
        return "STUDENT_PERSONAL";
    }
    
    @Override
    protected void validateParams(Object... params) {
        if (params.length != 3) {
            throw new IllegalArgumentException(
                "Se requieren 3 parámetros: studentId (UUID), startDate (LocalDate), endDate (LocalDate)");
        }
        
        if (!(params[0] instanceof UUID)) {
            throw new IllegalArgumentException("El primer parámetro debe ser un UUID (studentId)");
        }
        
        if (!(params[1] instanceof LocalDate)) {
            throw new IllegalArgumentException("El segundo parámetro debe ser LocalDate (startDate)");
        }
        
        if (!(params[2] instanceof LocalDate)) {
            throw new IllegalArgumentException("El tercer parámetro debe ser LocalDate (endDate)");
        }
    }
    
    @Override
    protected String getReportTitle() {
        return "REPORTE PERSONAL DE ASISTENCIA";
    }
    
    @Override
    protected Map<String, String> buildHeaderInfo(Object... params) {
        UUID studentId = (UUID) params[0];
        LocalDate startDate = (LocalDate) params[1];
        LocalDate endDate = (LocalDate) params[2];
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
        
        Map<String, String> info = createOrderedMap();
        info.put("Estudiante", student.getFullName());
        info.put("Email", student.getEmail());
        info.put("Período", 
                startDate.format(dateTimeFormatters.getDateFormatter()) + 
                " - " + 
                endDate.format(dateTimeFormatters.getDateFormatter()));
        info.put("Fecha de generación", 
                LocalDateTime.now().format(dateTimeFormatters.getDateTimeFormatter()));
        
        log.debug("Header info generado para estudiante: {}", student.getFullName());
        return info;
    }
    
    @Override
    protected Map<String, Object> buildStatistics(Object... params) {
        UUID studentId = (UUID) params[0];
        LocalDate startDate = (LocalDate) params[1];
        LocalDate endDate = (LocalDate) params[2];
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        log.info("Buscando asistencias para estudiante: {}", studentId);
        log.info("Rango de fechas: {} hasta {}", startDateTime, endDateTime);
        
        List<Attendance> attendances = attendanceRepository
                .findByStudentIdAndDeviceTimeBetween(studentId, startDateTime, endDateTime);
        
        log.info("Asistencias encontradas: {}", attendances.size());
        
        if (attendances.isEmpty()) {
            log.warn("No se encontraron asistencias para el estudiante {} en el período especificado", studentId);
            // Buscar todas las asistencias del estudiante para debugging
            List<Attendance> allAttendances = attendanceRepository.findAll().stream()
                    .filter(a -> a.getStudent().getId().equals(studentId))
                    .toList();
            log.warn("Total de asistencias del estudiante en toda la base de datos: {}", allAttendances.size());
            if (!allAttendances.isEmpty()) {
                log.warn("Primera asistencia: {}", allAttendances.get(0).getDeviceTime());
                log.warn("Última asistencia: {}", allAttendances.get(allAttendances.size() - 1).getDeviceTime());
            }
        }
        
        long totalSessions = sessionRepository
                .countByStartTimeBetween(startDateTime, endDateTime);
        
        log.info("Total de sesiones en el período: {}", totalSessions);
        
        long attendedSessions = attendances.size();
        double attendanceRate = totalSessions > 0
                ? (attendedSessions * 100.0) / totalSessions
                : 0.0;
        
        Map<String, Object> stats = createOrderedStatsMap();
        stats.put("Total de sesiones en el período", totalSessions);
        stats.put("Sesiones asistidas", attendedSessions);
        stats.put("Porcentaje de asistencia", String.format("%.2f%%", attendanceRate));
        
        log.debug("Estadísticas calculadas: {}% de asistencia", attendanceRate);
        return stats;
    }
    
    @Override
    protected PdfTableData buildTableData(Object... params) {
        UUID studentId = (UUID) params[0];
        LocalDate startDate = (LocalDate) params[1];
        LocalDate endDate = (LocalDate) params[2];
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<Attendance> attendances = attendanceRepository
                .findByStudentIdAndDeviceTimeBetween(studentId, startDateTime, endDateTime);
        
        List<String> headers = List.of(
                "Docente", 
                "Zona", 
                "Fecha/Hora Registro", 
                "Ubicación", 
                "Sync"
        );
        
        List<List<String>> rows = new ArrayList<>();
        
        for (Attendance attendance : attendances) {
            List<String> row = new ArrayList<>();
            row.add(attendance.getSession().getTeacher().getFullName());
            row.add(attendance.getSession().getGeofence().getName());
            row.add(attendance.getDeviceTime().format(dateTimeFormatters.getDateTimeFormatter()));
            row.add(attendance.getWithinGeofence() ? "✓ Dentro" : "✗ Fuera");
            
            long syncDelay = ChronoUnit.SECONDS.between(
                    attendance.getDeviceTime(),
                    attendance.getServerTime()
            );
            row.add(syncDelay + "s");
            
            rows.add(row);
        }
        
        log.debug("Tabla generada con {} filas de asistencias", rows.size());
        
        return PdfTableData.builder()
                .tableTitle("DETALLE DE ASISTENCIAS")
                .headers(headers)
                .rows(rows)
                .columnWidths(List.of(2, 2, 2, 2, 1))
                .emptyMessage("No se registraron asistencias en este período.")
                .build();
    }
}
