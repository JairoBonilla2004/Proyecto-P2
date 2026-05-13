package ec.edu.espe.Asistencia_con_Geofencing.service.pdf.strategy;

import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfReportContent;


public interface PdfReportStrategy {
    PdfReportContent generateReportContent(Object... params);
    String getReportType();
}
