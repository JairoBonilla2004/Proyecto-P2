package ec.edu.espe.Asistencia_con_Geofencing.service.report;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.GenerateReportRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ReportResponse;
import ec.edu.espe.Asistencia_con_Geofencing.model.ReportRequest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;


public interface ReportService {

    ReportResponse generateReport(GenerateReportRequest request, UUID userId);
    Page<ReportResponse> getMyReports(UUID userId, Pageable pageable);
    ReportRequest getReportById(UUID reportId, UUID userId);
    Resource downloadReport(UUID reportId, UUID userId);
    void deleteReport(UUID reportId, UUID userId);
}
