package ec.edu.espe.Asistencia_con_Geofencing.service.pdf;

import java.time.LocalDate;
import java.util.UUID;


public interface PdfGeneratorService {
    String generateStudentPersonalReport(UUID studentId, LocalDate startDate, LocalDate endDate);
    String generateSessionAttendanceReport(UUID sessionId);
}
