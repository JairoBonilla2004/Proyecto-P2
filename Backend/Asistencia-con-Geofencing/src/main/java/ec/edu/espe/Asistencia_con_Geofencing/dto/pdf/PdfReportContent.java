package ec.edu.espe.Asistencia_con_Geofencing.dto.pdf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfReportContent {
    private String title;
    private PdfMetadata metadata;
    private Map<String, String> headerInfo;
    private Map<String, Object> statistics;
    private PdfTableData tableData;
    private String footerText;
}
