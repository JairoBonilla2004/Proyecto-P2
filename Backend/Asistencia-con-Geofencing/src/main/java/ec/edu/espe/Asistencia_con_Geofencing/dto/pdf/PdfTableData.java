package ec.edu.espe.Asistencia_con_Geofencing.dto.pdf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfTableData {
    private String tableTitle;
    private List<String> headers;
    private List<List<String>> rows;
    private List<Integer> columnWidths;
    private String emptyMessage;
}
