package ec.edu.espe.SecureFrameGallery.application.config;
import java.io.*;
import java.sql.*;
import java.security.*;
import java.util.Base64;

public class inseguro {

    // 1. Hardcoded Credentials (exposición de secretos)
    private static final String DB_PASSWORD = "admin_password_super_secreto_123";

    public void manejarDatos(String userProvidedData, String fileName) throws Exception {
        
        // 2. SQL Injection (concatenación de strings en queries)
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", DB_PASSWORD);
        Statement st = con.createStatement();
        String query = "SELECT * FROM users WHERE username = '" + userProvidedData + "'";
        st.executeQuery(query);

        // 3. Path Traversal (manipulación de rutas de archivos)
        File file = new File("/var/www/uploads/" + fileName);
        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            // ... procesamiento
        }

        // 4. Weak Hashing (Uso de MD5 para contraseñas)
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(userProvidedData.getBytes());
        
        // 5. Command Injection (Ejecución de comandos del sistema con entrada externa)
        Runtime.getRuntime().exec("echo " + userProvidedData);

        // 6. Insecure Deserialization (Lectura de objetos sin validación)
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
        ois.readObject();

        // 7. Desactivación de validación de certificados SSL (posible Man-in-the-Middle)
        // (Pseudocódigo común en configuraciones inseguras de clientes HTTP)
        System.setProperty("javax.net.ssl.trustStore", "none");
    }
}