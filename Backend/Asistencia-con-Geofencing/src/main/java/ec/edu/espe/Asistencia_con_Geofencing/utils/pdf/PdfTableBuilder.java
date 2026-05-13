package ec.edu.espe.Asistencia_con_Geofencing.utils.pdf;

import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfTableData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class PdfTableBuilder {
    
    private final PdfStyler styler;

    public Table buildTable(PdfTableData tableData) {
        float[] columnWidths = tableData.getColumnWidths() != null
                ? convertToFloatArray(tableData.getColumnWidths())
                : createEqualWidths(tableData.getHeaders().size());
        
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        
        for (String header : tableData.getHeaders()) {
            table.addHeaderCell(styler.createHeaderCell(header));
        }
        for (List<String> row : tableData.getRows()) {
            for (String cellValue : row) {
                table.addCell(styler.createNormalCell(cellValue != null ? cellValue : ""));
            }
        }
        
        return table;
    }
    

    private float[] convertToFloatArray(List<Integer> widths) {
        float[] result = new float[widths.size()];
        for (int i = 0; i < widths.size(); i++) {
            result[i] = widths.get(i).floatValue();
        }
        return result;
    }

    private float[] createEqualWidths(int columnCount) {
        float[] widths = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            widths[i] = 1f;
        }
        return widths;
    }
}
