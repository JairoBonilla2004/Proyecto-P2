package ec.edu.espe.SecureFrameGallery.application.config;

import java.io.*;
import java.util.Random;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

public class VulnerableApp2 {

    private static final String API_KEY = "MY_SECRET_API_KEY";

    // 1. Criptografía Rota y Random Inseguro (Debería activar _PAT_WEAK_HASH_PY / _PAT_WEAK_CRYPTO_JJ)
    public String generateToken() throws Exception {
        Random random = new Random(); // Inseguro
        MessageDigest md = MessageDigest.getInstance("MD5"); // MD5 está roto, sumará al git s
        byte[] hash = md.digest(("TOKEN-" + random.nextInt()).getBytes());
        return hash.toString();
    }

    // 2. Inyección SQL clásica por concatenación directa (Debería activar _PAT_SQL_CONCAT_JJ)
    public void deleteProduct(String productId) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "admin123");
            Statement stmt = conn.createStatement();
            // ¡Peligro! Concatenación directa de strings sin PreparedStatement
            String query = "DELETE FROM products WHERE id = '" + productId + "'";
            stmt.executeUpdate(query);
        } catch (Exception e) {
            e.printStackTrace(); // Divulgación de errores
        }
    }

    // 3. Inyección de Comandos del Sistema Operativo (Debería activar _PAT_CMD_EXEC_JJ y ProcessBuilder)
    public void executeCommand(String clientInput) {
        try {
            // Caso A: Uso de Runtime.getRuntime().exec()
            Runtime rt = Runtime.getRuntime();
            Process p1 = rt.exec("ping -c 3 " + clientInput); 

            // Caso B: Uso de ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", clientInput);
            pb.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // 4. Path Traversal / Arbitrary File Read (Debería activar _PAT_PATH_TRAVERSAL_JJ)
    public void uploadFile(String userProvidedPath) {
        // Al concatenar el input del usuario directamente en la instancia del File, 
        // un atacante puede usar "../../../etc/passwd"
        File file = new File("/var/uploads/" + userProvidedPath);
        System.out.println("Uploading or accessing: " + file.getAbsolutePath());
        
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            System.out.println(br.readLine());
        } catch (Exception e) {
            System.out.println("Error processing file");
        }
    }

    // 5. Deserialización Insegura de Objetos (Debería activar _PAT_DESERIALIZATION_JJ)
    public Object processSerializedData(byte[] incomingData) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(incomingData);
            // El uso de ObjectInputStream con datos no confiables abre la puerta a RCE
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    // 6. Falta de validación de entrada e información sensible embebida
    public void createUser(String username) {
        System.out.println("Creating user: " + username);
    }

    public void debugMode() {
        System.out.println("DEBUG MODE ENABLED");
        System.out.println("API KEY: " + API_KEY);
    }

    public String getEndpoint() {
        return "http://internal-service.local/api";
    }

    public void showConfig() {
        System.out.println("Database password: admin123");
    }
}