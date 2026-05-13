package ec.edu.espe.Asistencia_con_Geofencing.dto.mapper;

import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ReportResponse;
import ec.edu.espe.Asistencia_con_Geofencing.model.ReportRequest;

public class ReportMapper {

    public static ReportResponse mapToResponse(ReportRequest report) {
        return ReportResponse.builder()
                .reportId(report.getId())
                .reportType(report.getReportType().name())
                .status(report.getStatus().name())
                .requestedAt(report.getRequestedAt())
                .filePath(report.getFilePath())
                .build();
    }
}
