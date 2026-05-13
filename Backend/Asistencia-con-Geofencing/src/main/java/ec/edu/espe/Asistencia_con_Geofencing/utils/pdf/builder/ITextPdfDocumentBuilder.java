package ec.edu.espe.Asistencia_con_Geofencing.utils.pdf.builder;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfMetadata;
import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfTableData;
import ec.edu.espe.Asistencia_con_Geofencing.utils.pdf.PdfStyler;
import ec.edu.espe.Asistencia_con_Geofencing.utils.pdf.PdfTableBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;


@Slf4j
@Component
public class ITextPdfDocumentBuilder implements PdfDocumentBuilder {
    
    private final PdfStyler styler;
    private final PdfTableBuilder tableBuilder;
    
    private String filePath;
    private Document document;
    private PdfDocument pdfDocument;
    
    public ITextPdfDocumentBuilder(PdfStyler styler, PdfTableBuilder tableBuilder) {
        this.styler = styler;
        this.tableBuilder = tableBuilder;
    }

    public ITextPdfDocumentBuilder initialize(String filePath) throws IOException {
        this.filePath = filePath;
        PdfWriter writer = new PdfWriter(filePath);
        this.pdfDocument = new PdfDocument(writer);
        this.document = new Document(pdfDocument);
        return this;
    }
    
    @Override
    public PdfDocumentBuilder withMetadata(PdfMetadata metadata) {
        if (metadata != null && pdfDocument != null) {
            var docInfo = pdfDocument.getDocumentInfo();
            if (metadata.getAuthor() != null) {
                docInfo.setAuthor(metadata.getAuthor());
            }
            if (metadata.getSubject() != null) {
                docInfo.setSubject(metadata.getSubject());
            }
            if (metadata.getCreator() != null) {
                docInfo.setCreator(metadata.getCreator());
            }
            if (metadata.getKeywords() != null) {
                docInfo.setKeywords(metadata.getKeywords());
            }
        }
        return this;
    }
    
    @Override
    public PdfDocumentBuilder withTitle(String title) {
        if (title != null && document != null) {
            document.add(styler.createTitleParagraph(title));
            document.add(styler.createSpacer());
        }
        return this;
    }
    
    @Override
    public PdfDocumentBuilder withHeaderInfo(String key, String value) {
        if (key != null && value != null && document != null) {
            String text = key + ": " + value;
            document.add(styler.createBoldParagraph(text));
        }
        return this;
    }
    
    @Override
    public PdfDocumentBuilder withStatistics(String title, Map<String, Object> statistics) {
        if (document != null) {
            document.add(styler.createSpacer());
            
            if (title != null) {
                document.add(styler.createSectionTitleParagraph(title));
            }
            
            if (statistics != null && !statistics.isEmpty()) {
                for (Map.Entry<String, Object> entry : statistics.entrySet()) {
                    String text = entry.getKey() + ": " + entry.getValue();
                    document.add(styler.createNormalParagraph(text));
                }
            }
        }
        return this;
    }
    
    @Override
    public PdfDocumentBuilder withTable(PdfTableData tableData) {
        if (document != null && tableData != null) {
            document.add(styler.createSpacer());
            
            if (tableData.getTableTitle() != null) {
                document.add(styler.createSectionTitleParagraph(tableData.getTableTitle()));
                document.add(styler.createSpacer());
            }
            
            if (tableData.getRows() != null && !tableData.getRows().isEmpty()) {
                document.add(tableBuilder.buildTable(tableData));
            } else if (tableData.getEmptyMessage() != null) {
                document.add(styler.createEmptyMessageParagraph(tableData.getEmptyMessage()));
            }
        }
        return this;
    }
    
    @Override
    public PdfDocumentBuilder withFooter(String footer) {
        if (footer != null && document != null) {
            document.add(styler.createSpacer());
            document.add(styler.createNormalParagraph(footer));
        }
        return this;
    }
    
    @Override
    public String build() throws IOException {
        if (document != null) {
            document.close();
            log.info("Documento PDF generado exitosamente: {}", filePath);
        }
        return filePath;
    }
}
