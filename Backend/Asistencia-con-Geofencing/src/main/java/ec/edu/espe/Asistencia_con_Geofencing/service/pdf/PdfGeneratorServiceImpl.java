package ec.edu.espe.Asistencia_con_Geofencing.service.pdf;

import ec.edu.espe.Asistencia_con_Geofencing.dto.pdf.PdfReportContent;
import ec.edu.espe.Asistencia_con_Geofencing.service.pdf.factory.PdfReportStrategyFactory;
import ec.edu.espe.Asistencia_con_Geofencing.service.pdf.strategy.PdfReportStrategy;
import ec.edu.espe.Asistencia_con_Geofencing.service.storage.StorageStrategy;
import ec.edu.espe.Asistencia_con_Geofencing.utils.pdf.PdfFileManager;
import ec.edu.espe.Asistencia_con_Geofencing.utils.pdf.builder.ITextPdfDocumentBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGeneratorServiceImpl implements PdfGeneratorService {
    
    private final ITextPdfDocumentBuilder documentBuilder;
    private final PdfReportStrategyFactory strategyFactory;
    private final PdfFileManager fileManager;
    private final StorageStrategy storageStrategy;
    
    @Override
    public String generateStudentPersonalReport(UUID studentId, LocalDate startDate, LocalDate endDate) {
        log.info("Iniciando generación de reporte personal para estudiante: {}", studentId);
        
        try {
            fileManager.ensureReportDirectoryExists();
            
            PdfReportStrategy strategy = strategyFactory.getStrategy("STUDENT_PERSONAL");
            PdfReportContent content = strategy.generateReportContent(studentId, startDate, endDate);
            
            String fileName = fileManager.generateFileName("reporte_personal", studentId.toString());
            String localFilePath = fileManager.getFullPath(fileName);
            
            buildPdfDocument(localFilePath, content);
            log.debug("PDF generado localmente: {}", localFilePath);
            
            String storageUrl = storageStrategy.uploadFile(localFilePath, fileName);
            log.info("Reporte personal generado y almacenado [{}]: {}",
                    storageStrategy.getStorageType(), storageUrl);
            
            if (!"local".equals(storageStrategy.getStorageType())) {
                fileManager.deleteFile(localFilePath);
            }
            
            return storageUrl;
            
        } catch (IOException e) {
            log.error(" Error de I/O generando reporte personal para estudiante: {}", studentId, e);
            throw new RuntimeException("Error generando reporte PDF: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error inesperado generando reporte personal para estudiante: {}", studentId, e);
            throw new RuntimeException("Error generando reporte PDF: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String generateSessionAttendanceReport(UUID sessionId) {
        log.info("Iniciando generación de reporte de sesión: {}", sessionId);
        
        try {
            fileManager.ensureReportDirectoryExists();
            PdfReportStrategy strategy = strategyFactory.getStrategy("SESSION_ATTENDANCE");
            PdfReportContent content = strategy.generateReportContent(sessionId);
            String fileName = fileManager.generateFileName("reporte_sesion", sessionId.toString());
            String localFilePath = fileManager.getFullPath(fileName);
            
            buildPdfDocument(localFilePath, content);
            log.debug(" PDF generado localmente: {}", localFilePath);
            
            // 5. Subir usando Storage Strategy Pattern
            String storageUrl = storageStrategy.uploadFile(localFilePath, fileName);
            log.info(" Reporte de sesión generado y almacenado [{}]: {}",
                    storageStrategy.getStorageType(), storageUrl);
            
            if (!"local".equals(storageStrategy.getStorageType())) {
                fileManager.deleteFile(localFilePath);
            }
            
            return storageUrl;
        } catch (IOException e) {
            log.error(" Error de I/O generando reporte de sesión: {}", sessionId, e);
            throw new RuntimeException("Error generando reporte PDF: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error inesperado generando reporte de sesión: {}", sessionId, e);
            throw new RuntimeException("Error generando reporte PDF: " + e.getMessage(), e);
        }
    }
    
    private void buildPdfDocument(String filePath, PdfReportContent content) throws IOException {
        documentBuilder
                .initialize(filePath)
                .withMetadata(content.getMetadata())
                .withTitle(content.getTitle())
                .withStatistics("RESUMEN", content.getStatistics())
                .withTable(content.getTableData())
                .withFooter(content.getFooterText())
                .build();
    }
}


