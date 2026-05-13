package ec.edu.espe.Asistencia_con_Geofencing.dto.pdf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfMetadata {
    private String author;
    private String subject;
    private String creator;
    private LocalDateTime creationDate;
    private String keywords;
}
