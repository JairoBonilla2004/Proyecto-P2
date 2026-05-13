package ec.edu.espe.Asistencia_con_Geofencing.service.report;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;


@Component
@Slf4j
public class ReportCleanupTask {
    
    @Value("${reports.directory:/tmp/reports}")
    private String reportsDirectory;
    
    @Value("${reports.retention.days:7}")
    private int retentionDays;
    

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldReports() {
        log.info("=== Iniciando limpieza de reportes antiguos ===");
        log.info("Directorio: {}", reportsDirectory);
        log.info("Retención: {} días", retentionDays);
        
        try {
            Path reportsPath = Paths.get(reportsDirectory);
            if (!Files.exists(reportsPath)) {
                log.warn("Directorio de reportes no existe: {}", reportsDirectory);
                return;
            }
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            log.info("Eliminando reportes anteriores a: {}", cutoffDate);
            
            int totalFiles = 0;
            int deletedFiles = 0;
            int errorFiles = 0;
            
            try (var stream = Files.walk(reportsPath)) {
                var pdfFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    .toList();
                
                totalFiles = pdfFiles.size();
                log.info("Total de archivos PDF encontrados: {}", totalFiles);
                
                for (Path path : pdfFiles) {
                    try {
                        FileTime lastModified = Files.getLastModifiedTime(path);
                        LocalDateTime fileDate = LocalDateTime.ofInstant(
                            lastModified.toInstant(), 
                            ZoneId.systemDefault()
                        );
                        
                        if (fileDate.isBefore(cutoffDate)) {
                            Files.delete(path);
                            deletedFiles++;
                            log.debug("Reporte eliminado: {} (fecha: {})", 
                                path.getFileName(), fileDate);
                        }
                    } catch (IOException e) {
                        errorFiles++;
                        log.error("Error procesando archivo: {}", path, e);
                    }
                }
            }
            
            // Resumen
            log.info("=== Limpieza de reportes completada ===");
            log.info("Total de archivos: {}", totalFiles);
            log.info("Archivos eliminados: {}", deletedFiles);
            log.info("Archivos con error: {}", errorFiles);
            log.info("Archivos conservados: {}", totalFiles - deletedFiles - errorFiles);
            
        } catch (Exception e) {
            log.error("Error crítico en la limpieza de reportes", e);
        }
    }

    public void executeManualCleanup() {
        log.info("Ejecutando limpieza manual de reportes...");
        cleanupOldReports();
    }
}
