package ec.edu.espe.Asistencia_con_Geofencing.service.report;
import ec.edu.espe.Asistencia_con_Geofencing.dto.mapper.ReportMapper;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.GenerateReportRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ReportResponse;
import ec.edu.espe.Asistencia_con_Geofencing.exception.ResourceNotFoundException;
import ec.edu.espe.Asistencia_con_Geofencing.model.AttendanceSession;
import ec.edu.espe.Asistencia_con_Geofencing.model.ReportRequest;
import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.ReportStatus;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.ReportType;
import ec.edu.espe.Asistencia_con_Geofencing.repository.AttendanceSessionRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.ReportRequestRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.UserRepository;
import ec.edu.espe.Asistencia_con_Geofencing.service.pdf.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRequestRepository reportRequestRepository;
    private final UserRepository userRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final PdfGeneratorService pdfGeneratorService;

    @Override
    @Transactional
    public ReportResponse generateReport(GenerateReportRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setRequestedBy(user);
        reportRequest.setReportType(ReportType.valueOf(request.getReportType()));
        reportRequest.setStatus(ReportStatus.GENERATING);

        ReportType reportType = ReportType.valueOf(request.getReportType());

        if (reportType == ReportType.STUDENT_PERSONAL) {
            reportRequest.setStudent(user);

            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new IllegalArgumentException("Las fechas son requeridas para reportes personales");
            }

        } else if (reportType == ReportType.SESSION_ATTENDANCE) {
            if (request.getSessionId() == null) {
                throw new IllegalArgumentException("El ID de sesión es requerido para reportes de asistencia");
            }

            AttendanceSession session = attendanceSessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada"));

            if (!session.getTeacher().getId().equals(userId)) {
                throw new RuntimeException("No tienes permiso para generar el reporte de esta sesión");
            }

            if (session.getActive()) {
                throw new IllegalArgumentException("No se puede generar el reporte de una sesión activa. Por favor, finaliza la sesión primero.");
            }

            reportRequest.setSession(session);
        }

        reportRequest = reportRequestRepository.save(reportRequest);

        // Lanzar job asíncrono para generar PDF
        UUID reportRequestId = reportRequest.getId();
        generatePdfAsync(reportRequestId, request);

        return ReportResponse.builder()
                .reportId(reportRequest.getId())
                .reportType(reportRequest.getReportType().name())
                .status(reportRequest.getStatus().name())
                .estimatedTime("30 seconds")
                .requestedAt(reportRequest.getRequestedAt())
                .build();
    }

    @Async("taskExecutor")
    @Transactional
    public void generatePdfAsync(UUID reportRequestId, GenerateReportRequest request) {
        log.info("Iniciando generación asíncrona de reporte ID: {}", reportRequestId);

        try {
            ReportRequest reportRequest = reportRequestRepository.findById(reportRequestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Solicitud de reporte no encontrada"));

            String filePath;
            if (reportRequest.getReportType() == ReportType.STUDENT_PERSONAL) {
                log.info("Generando reporte personal para estudiante: {}", reportRequest.getStudent().getId());

                filePath = pdfGeneratorService.generateStudentPersonalReport(
                        reportRequest.getStudent().getId(),
                        request.getStartDate(),
                        request.getEndDate()
                );
            } else {
                log.info("Generando reporte de sesión: {}", reportRequest.getSession().getId());

                // Generar reporte de asistencias de la sesión
                filePath = pdfGeneratorService.generateSessionAttendanceReport(
                        reportRequest.getSession().getId()
                );
            }

            reportRequest.setFilePath(filePath);
            reportRequest.setStatus(ReportStatus.COMPLETED);
            reportRequestRepository.save(reportRequest);

            log.info("Reporte generado exitosamente: {}", reportRequestId);
            log.info("Archivo guardado en: {}", filePath);
            log.info("Archivo existe: {}", Files.exists(Paths.get(filePath)));

        } catch (Exception e) {
            log.error("Error generando el reporte {}: {}", reportRequestId, e.getMessage(), e);

            try {
                ReportRequest reportRequest = reportRequestRepository.findById(reportRequestId).orElse(null);
                if (reportRequest != null) {
                    reportRequest.setStatus(ReportStatus.FAILED);
                    reportRequestRepository.save(reportRequest);
                }
            } catch (Exception updateEx) {
                log.error("Error actualizando estado del reporte fallido: {}", updateEx.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getMyReports(UUID userId, Pageable pageable) {
        return reportRequestRepository.findByRequestedById(userId, pageable)
                .map(ReportMapper::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportRequest getReportById(UUID reportId, UUID userId) {
        ReportRequest report = reportRequestRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado"));

        if (!report.getRequestedBy().getId().equals(userId)) {
            throw new RuntimeException("No tienes permiso para acceder a este reporte");
        }

        return report;
    }

    @Override
    public Resource downloadReport(UUID reportId, UUID userId) {
        ReportRequest report = getReportById(reportId, userId);

        if (report.getFilePath() == null) {
            throw new RuntimeException("El reporte aún no está disponible");
        }

        if (report.getStatus() == ReportStatus.FAILED) {
            throw new RuntimeException("El reporte falló al generarse");
        }

        if (report.getStatus() == ReportStatus.GENERATING) {
            throw new RuntimeException("El reporte aún se está generando");
        }

        try {
            String filePath = report.getFilePath();
            
            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                log.info(" Descargando reporte desde URL: {}", filePath);
                Resource resource = new UrlResource(filePath);
                
                if (!resource.exists()) {
                    log.error(" Recurso no encontrado en URL: {}", filePath);
                    throw new RuntimeException("Archivo no encontrado en Supabase");
                }
                
                log.info(" Reporte disponible en: {}", filePath);
                return resource;
            }
            
        
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            log.info("Intentando acceder al archivo local en: {}", filePath);
            log.info("Path absoluto: {}", path.toAbsolutePath());
            log.info("Archivo existe: {}", Files.exists(path));

            if (!resource.exists()) {
                log.error("Archivo no encontrado en: {}", path.toAbsolutePath());
                throw new RuntimeException("Archivo no encontrado");
            }

            if (!resource.isReadable()) {
                log.error("Archivo no es legible: {}", path.toAbsolutePath());
                throw new RuntimeException("Archivo no es legible");
            }

            log.info("Archivo encontrado y legible: {}", path.toAbsolutePath());
            return resource;

        } catch (Exception e) {
            log.error("Error accediendo al archivo del reporte: {}", e.getMessage());
            throw new RuntimeException("Error accediendo al archivo del reporte");
        }
    }

    @Override
    @Transactional
    public void deleteReport(UUID reportId, UUID userId) {
        ReportRequest report = reportRequestRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado"));

        // Validar que el usuario sea el dueño del reporte
        if (!report.getRequestedBy().getId().equals(userId)) {
            throw new RuntimeException("No tienes permiso para eliminar este reporte");
        }

        // Eliminar el archivo físico si existe
        if (report.getFilePath() != null) {
            try {
                Path path = Paths.get(report.getFilePath());
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.info("Archivo de reporte eliminado: {}", report.getFilePath());
                }
            } catch (Exception e) {
                log.error("Error eliminando archivo físico del reporte: {}", e.getMessage());
                // Continuar con la eliminación del registro en BD aunque falle el archivo
            }
        }

        // Eliminar el registro de la base de datos
        reportRequestRepository.delete(report);
        log.info("Registro de reporte eliminado: {}", reportId);
    }
}
