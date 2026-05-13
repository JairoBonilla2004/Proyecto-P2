package ec.edu.espe.Asistencia_con_Geofencing.service.storage;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@Service
@ConditionalOnProperty(name = "reports.storage.type", havingValue = "supabase")
@Slf4j
public class SupabaseStorageService implements StorageStrategy {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${reports.storage.bucket-name}")
    private String bucketName;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public String uploadFile(String localFilePath, String destinationFileName) throws IOException {
        log.info("Subiendo archivo: {}", destinationFileName);

        Path filePath = Path.of(localFilePath);
        if (!Files.exists(filePath)) {
            throw new IOException("El archivo no existe: " + localFilePath);
        }

        byte[] fileBytes = Files.readAllBytes(filePath);
        String uploadUrl = String.format("%s/storage/v1/object/%s/%s",
                supabaseUrl, bucketName, destinationFileName);
        RequestBody body = RequestBody.create(fileBytes, MediaType.parse("application/pdf"));
        Request request = new Request.Builder()
                .url(uploadUrl)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/pdf")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Sin detalle";
                log.error("Error subiendo archivo: {} - {}", response.code(), errorBody);
                throw new IOException("Error subiendo archivo a Supabase: " + response.code() + " - " + errorBody);
            }

            log.info("Archivo subido exitosamente: {}", destinationFileName);
            return getPublicUrl(destinationFileName);
        }
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
        log.info("Eliminando archivo: {}", fileName);

        String deleteUrl = String.format("%s/storage/v1/object/%s/%s",
                supabaseUrl, bucketName, fileName);

        Request request = new Request.Builder()
                .url(deleteUrl)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .delete()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Sin detalle";
                log.warn("Error eliminando archivo: {} - {}", response.code(), errorBody);
                throw new IOException("Error eliminando archivo de Supabase: " + response.code());
            }

            log.info("Archivo eliminado: {}", fileName);
        }
    }

    @Override
    public String getPublicUrl(String fileName) {
        String publicUrl = String.format("%s/storage/v1/object/public/%s/%s",
                supabaseUrl, bucketName, fileName);
        log.debug("URL pública: {}", publicUrl);
        return publicUrl;
    }

    @Override
    public boolean isConfigured() {
        boolean configured = supabaseUrl != null && !supabaseUrl.isEmpty()
                && supabaseKey != null && !supabaseKey.isEmpty()
                && bucketName != null && !bucketName.isEmpty();
        
        if (!configured) {
            log.warn("Storage no está completamente configurado");
        }
        
        return configured;
    }

    @Override
    public String getStorageType() {
        return "supabase";
    }
}
