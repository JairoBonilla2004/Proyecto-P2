package ec.edu.espe.Asistencia_con_Geofencing.utils.pdf.builder;

import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfMetadata;
import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfTableData;

import java.io.IOException;


public interface PdfDocumentBuilder {
    PdfDocumentBuilder withMetadata(PdfMetadata metadata);
    PdfDocumentBuilder withTitle(String title);
    PdfDocumentBuilder withHeaderInfo(String key, String value);
    PdfDocumentBuilder withStatistics(String title, java.util.Map<String, Object> statistics);
    PdfDocumentBuilder withTable(PdfTableData tableData);
    PdfDocumentBuilder withFooter(String footer);
    String build() throws IOException;
}
