package ec.edu.espe.Asistencia_con_Geofencing.utils.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad para gestión de archivos PDF en el sistema local.
 * 
 * Responsabilidad ÚNICA: Manejo de archivos temporales locales para generación de PDFs
 * - Genera nombres de archivo únicos
 * - Gestiona directorios temporales
 * - Operaciones básicas de archivos (existe, eliminar)
 * 
 * NO maneja lógica de storage remoto (Supabase, S3, etc.)
 * Esa responsabilidad está en StorageStrategy implementations
 */
@Component
@Slf4j
public class PdfFileManager {
    
    private static final String TEMP_STORAGE_PATH = System.getProperty("java.io.tmpdir") + "/reports";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Asegura que el directorio temporal de reportes exista
     */
    public void ensureReportDirectoryExists() throws IOException {
        Path reportsDir = Paths.get(TEMP_STORAGE_PATH);
        if (!Files.exists(reportsDir)) {
            Files.createDirectories(reportsDir);
            log.info("📁 Directorio temporal de reportes creado: {}", reportsDir);
        }
    }

    /**
     * Genera un nombre de archivo único con timestamp
     */
    public String generateFileName(String prefix, String identifier) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        return String.format("%s_%s_%s.pdf", prefix, identifier, timestamp);
    }

    /**
     * Obtiene la ruta completa temporal para un archivo
     */
    public String getFullPath(String fileName) {
        return Paths.get(TEMP_STORAGE_PATH, fileName).toString();
    }

    /**
     * Verifica si un archivo existe en el sistema local
     */
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Elimina un archivo del sistema local
     */
    public void deleteFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            Files.deleteIfExists(path);
            log.info("🗑️ Archivo local eliminado: {}", filePath);
        }
    }
}

