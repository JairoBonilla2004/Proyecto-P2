package ec.edu.espe.Asistencia_con_Geofencing.service.pdf.strategy;

import ec.edu.espe.Asistencia_con_Geofencing.exception.ResourceNotFoundException;
import ec.edu.espe.Asistencia_con_Geofencing.model.Attendance;
import ec.edu.espe.Asistencia_con_Geofencing.model.AttendanceSession;
import ec.edu.espe.Asistencia_con_Geofencing.repository.AttendanceRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.AttendanceSessionRepository;
import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfTableData;
import ec.edu.espe.Asistencia_con_Geofencing.utils.pdf.DateTimeFormatters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
@Slf4j
public class SessionAttendanceReportStrategy extends AbstractPdfReportStrategy {
    
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository sessionRepository;
    
    public SessionAttendanceReportStrategy(
            DateTimeFormatters dateTimeFormatters,
            AttendanceRepository attendanceRepository,
            AttendanceSessionRepository sessionRepository) {
        super(dateTimeFormatters);
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
    }
    
    @Override
    public String getReportType() {
        return "SESSION_ATTENDANCE";
    }
    
    @Override
    protected void validateParams(Object... params) {
        if (params.length != 1) {
            throw new IllegalArgumentException("Se requiere 1 parámetro: sessionId (UUID)");
        }
        
        if (!(params[0] instanceof UUID)) {
            throw new IllegalArgumentException("El parámetro debe ser un UUID (sessionId)");
        }
    }
    
    @Override
    protected String getReportTitle() {
        return "REPORTE DE ASISTENCIA - SESIÓN";
    }
    
    @Override
    protected Map<String, String> buildHeaderInfo(Object... params) {
        UUID sessionId = (UUID) params[0];
        
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada"));
        
        String duration = calculateDuration(session);
        
        Map<String, String> info = createOrderedMap();
        info.put("Docente", session.getTeacher().getFullName());
        info.put("Zona", session.getGeofence().getName());
        info.put("Inicio", session.getStartTime().format(dateTimeFormatters.getDateTimeFormatter()));
        
        if (session.getEndTime() != null) {
            info.put("Fin", session.getEndTime().format(dateTimeFormatters.getDateTimeFormatter()));
        }
        
        info.put("Duración", duration);
        info.put("Estado", session.getActive() ? "Activa" : "Finalizada");
        
        log.debug("Header info generado para sesión: {}", sessionId);
        return info;
    }
    
    @Override
    protected Map<String, Object> buildStatistics(Object... params) {
        UUID sessionId = (UUID) params[0];
        
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada"));
        
        List<Attendance> attendances = attendanceRepository.findBySessionId(sessionId);
        
        long onTimeRegistrations = attendances.stream()
                .filter(a -> ChronoUnit.MINUTES.between(session.getStartTime(), a.getDeviceTime()) <= 15)
                .count();
        
        long lateRegistrations = attendances.size() - onTimeRegistrations;
        
        double avgSyncDelay = attendances.stream()
                .mapToLong(a -> ChronoUnit.SECONDS.between(a.getDeviceTime(), a.getServerTime()))
                .average()
                .orElse(0.0);
        
        long offlineSyncs = attendances.stream()
                .filter(a -> ChronoUnit.MINUTES.between(a.getDeviceTime(), a.getServerTime()) > 1)
                .count();
        
        long withinGeofence = attendances.stream()
                .filter(Attendance::getWithinGeofence)
                .count();
        
        Map<String, Object> stats = createOrderedStatsMap();
        stats.put("Total de asistencias", attendances.size());
        stats.put("Registros puntuales (≤15 min)", onTimeRegistrations);
        stats.put("Registros tardíos (>15 min)", lateRegistrations);
        stats.put("Tiempo promedio de sincronización", String.format("%.1f segundos", avgSyncDelay));
        stats.put("Sincronizaciones offline (>1 min)", offlineSyncs);
        stats.put("Dentro de geofence", withinGeofence);
        
        log.debug("Estadísticas calculadas para sesión: {} asistencias", attendances.size());
        return stats;
    }
    
    @Override
    protected PdfTableData buildTableData(Object... params) {
        UUID sessionId = (UUID) params[0];
        
        List<Attendance> attendances = attendanceRepository.findBySessionId(sessionId);
        
        List<String> headers = List.of(
                "#", 
                "Estudiante", 
                "Hora Registro", 
                "Hora Servidor", 
                "Geofence", 
                "Delay"
        );
        
        List<List<String>> rows = new ArrayList<>();
        
        int index = 1;
        for (Attendance attendance : attendances) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(index++));
            row.add(attendance.getStudent().getFullName());
            row.add(attendance.getDeviceTime().format(dateTimeFormatters.getDateTimeFormatter()));
            row.add(attendance.getServerTime().format(dateTimeFormatters.getDateTimeFormatter()));
            row.add(attendance.getWithinGeofence() ? "✓" : "✗");
            
            long syncDelay = ChronoUnit.SECONDS.between(
                    attendance.getDeviceTime(),
                    attendance.getServerTime()
            );
            row.add(syncDelay + "s");
            
            rows.add(row);
        }
        
        log.debug("Tabla generada con {} filas de asistencias", rows.size());
        
        return PdfTableData.builder()
                .tableTitle("LISTA DE ASISTENCIA")
                .headers(headers)
                .rows(rows)
                .columnWidths(List.of(1, 3, 2, 2, 1, 1))
                .emptyMessage("No se han registrado asistencias aún.")
                .build();
    }

    private String calculateDuration(AttendanceSession session) {
        if (session.getEndTime() == null) {
            return "En curso";
        }
        
        long minutes = ChronoUnit.MINUTES.between(session.getStartTime(), session.getEndTime());
        long hours = minutes / 60;
        long mins = minutes % 60;
        
        return String.format("%d horas %d minutos", hours, mins);
    }
}
