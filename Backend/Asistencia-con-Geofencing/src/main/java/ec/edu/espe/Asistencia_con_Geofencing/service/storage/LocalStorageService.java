package ec.edu.espe.Asistencia_con_Geofencing.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Service
@ConditionalOnProperty(name = "reports.storage.type", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalStorageService implements StorageStrategy {
    
    private static final String DEFAULT_STORAGE_PATH = System.getProperty("java.io.tmpdir") + "/reports";
    
    @Override
    public String uploadFile(String localFilePath, String destinationFileName) throws IOException {
        log.info("[Local] Archivo ya está en storage local: {}", localFilePath);
        return localFilePath;
    }
    
    @Override
    public void deleteFile(String fileName) throws IOException {
        Path filePath = Paths.get(fileName);
        if (Files.exists(filePath)) {
            Files.deleteIfExists(filePath);
            log.info("[Local] Archivo eliminado: {}", fileName);
        } else {
            log.warn("[Local] Archivo no existe: {}", fileName);
        }
    }
    
    @Override
    public String getPublicUrl(String fileName) {
        log.debug("Ruta de archivo: {}", fileName);
        return fileName;
    }
    
    @Override
    public boolean isConfigured() {
        try {
            Path storageDir = Paths.get(DEFAULT_STORAGE_PATH);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
                log.info("Directorio de storage creado: {}", storageDir);
            }
            return true;
        } catch (IOException e) {
            log.error("Error configurando directorio de storage", e);
            return false;
        }
    }
    
    @Override
    public String getStorageType() {
        return "local";
    }
}
