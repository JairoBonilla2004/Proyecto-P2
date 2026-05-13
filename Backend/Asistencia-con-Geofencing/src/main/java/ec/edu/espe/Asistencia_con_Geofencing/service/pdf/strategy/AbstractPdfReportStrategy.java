package ec.edu.espe.Asistencia_con_Geofencing.service.pdf.strategy;

import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfMetadata;
import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfReportContent;
import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfTableData;
import ec.edu.espe.Asistencia_con_Geofencing.utils.pdf.DateTimeFormatters;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;


public abstract class AbstractPdfReportStrategy implements PdfReportStrategy {
    
    protected final DateTimeFormatters dateTimeFormatters;
    
    protected AbstractPdfReportStrategy(DateTimeFormatters dateTimeFormatters) {
        this.dateTimeFormatters = dateTimeFormatters;
    }
    
    @Override
    public PdfReportContent generateReportContent(Object... params) {
        validateParams(params);
        
        return PdfReportContent.builder()
                .title(getReportTitle())
                .metadata(buildMetadata())
                .headerInfo(buildHeaderInfo(params))
                .statistics(buildStatistics(params))
                .tableData(buildTableData(params))
                .footerText(buildFooter())
                .build();
    }
    
    protected abstract void validateParams(Object... params);

    protected abstract String getReportTitle();

    protected abstract Map<String, String> buildHeaderInfo(Object... params);

    protected abstract Map<String, Object> buildStatistics(Object... params);

    protected abstract PdfTableData buildTableData(Object... params);

    protected PdfMetadata buildMetadata() {
        return PdfMetadata.builder()
                .author("Sistema de Asistencia con Geofencing")
                .creator("iText PDF Generator")
                .subject("Reporte de Asistencia")
                .creationDate(LocalDateTime.now())
                .keywords("asistencia, reporte, geofencing")
                .build();
    }

    protected String buildFooter() {
        return "Generado el: " + LocalDateTime.now().format(dateTimeFormatters.getDateTimeFormatter());
    }

    protected Map<String, String> createOrderedMap() {
        return new LinkedHashMap<>();
    }

    protected Map<String, Object> createOrderedStatsMap() {
        return new LinkedHashMap<>();
    }
}
