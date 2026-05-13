package ec.edu.espe.Asistencia_con_Geofencing.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${fcm.service-account-file:}")
    private String serviceAccountFile;

    @Value("${fcm.enabled:false}")
    private boolean firebaseEnabled;

    @PostConstruct
    public void initialize() {
        if (!firebaseEnabled) {
            log.warn("Firebase está deshabilitado. Las notificaciones push no se enviarán.");
            return;
        }

        if (serviceAccountFile == null || serviceAccountFile.isEmpty()) {
            log.warn("No se ha configurado el archivo de credenciales de Firebase (firebase.service-account-file). " +
                    "Las notificaciones push funcionarán en modo simulación.");
            return;
        }

        try {
            Resource resource = new ClassPathResource(serviceAccountFile);
            InputStream serviceAccount;
            
            if (resource.exists()) {
                serviceAccount = resource.getInputStream();
            } else {
                serviceAccount = new FileInputStream(serviceAccountFile);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK inicializado correctamente");
            }

        } catch (IOException e) {
            log.error("Error al inicializar Firebase Admin SDK: {}. " +
                     "Las notificaciones push funcionarán en modo simulación.", e.getMessage());
        }
    }
}
