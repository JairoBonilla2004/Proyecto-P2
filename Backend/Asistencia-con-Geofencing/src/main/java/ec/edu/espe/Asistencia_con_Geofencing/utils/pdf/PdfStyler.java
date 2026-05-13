package ec.edu.espe.Asistencia_con_Geofencing.utils.pdf;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Component;


@Component
public class PdfStyler {
    public static final float TITLE_FONT_SIZE = 20f;
    public static final float SECTION_TITLE_FONT_SIZE = 14f;
    public static final float NORMAL_FONT_SIZE = 12f;

    public Paragraph createTitleParagraph(String text) {
        return new Paragraph(text)
                .setFontSize(TITLE_FONT_SIZE)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
    }

    public Paragraph createSectionTitleParagraph(String text) {
        return new Paragraph(text)
                .setFontSize(SECTION_TITLE_FONT_SIZE)
                .setBold();
    }

    public Paragraph createNormalParagraph(String text) {
        return new Paragraph(text)
                .setFontSize(NORMAL_FONT_SIZE);
    }

    public Paragraph createBoldParagraph(String text) {
        return new Paragraph(text)
                .setFontSize(NORMAL_FONT_SIZE)
                .setBold();
    }

    public Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(NORMAL_FONT_SIZE);
    }

    public Cell createNormalCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setFontSize(NORMAL_FONT_SIZE);
    }

    public Cell createHighlightedCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setFontSize(NORMAL_FONT_SIZE);
    }

    public Cell createCenteredCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(NORMAL_FONT_SIZE);
    }

    public Paragraph createEmptyMessageParagraph(String message) {
        return new Paragraph(message)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(NORMAL_FONT_SIZE);
    }

    public Paragraph createSpacer() {
        return new Paragraph("\n");
    }
}
