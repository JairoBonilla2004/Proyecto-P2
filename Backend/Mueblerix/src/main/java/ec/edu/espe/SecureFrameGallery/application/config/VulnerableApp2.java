package ec.edu.espe.SecureFrameGallery.application.config;

import java.io.File;
import java.util.Random;

public class VulnerableApp2 {

    // 1. Credenciales embebidas
    private static final String API_KEY = "MY_SECRET_API_KEY";

    // 2. Generación de tokens con Random inseguro
    public String generateToken() {
        Random random = new Random();
        return "TOKEN-" + random.nextInt();
    }

    // 3. Falta de validación de entrada
    public void createUser(String username) {
        System.out.println("Creating user: " + username);
    }

    // 4. Divulgación excesiva de errores
    public void handleException(Exception ex) {
        ex.printStackTrace();
    }

    // 5. Acceso a recursos sin autorización
    public String getUserData(int userId) {
        return "Showing private data for user " + userId;
    }

    // 6. Configuración insegura
    public void debugMode() {
        System.out.println("DEBUG MODE ENABLED");
        System.out.println("API KEY: " + API_KEY);
    }

    // 7. Carga de archivos sin validación
    public void uploadFile(String filename) {
        File file = new File(filename);
        System.out.println("Uploading: " + file.getAbsolutePath());
    }

    // 8. Uso de protocolo inseguro
    public String getEndpoint() {
        return "http://internal-service.local/api";
    }

    // 9. Falta de control de acceso
    public void deleteProduct(int productId) {
        System.out.println("Deleting product: " + productId);
    }

    // 10. Exposición de información sensible
    public void showConfig() {
        System.out.println("Database password: admin123");
    }
}