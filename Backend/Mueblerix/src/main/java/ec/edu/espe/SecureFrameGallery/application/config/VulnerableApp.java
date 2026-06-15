package ec.edu.espe.SecureFrameGallery.application.config;

import java.io.*;
import java.sql.*;

public class VulnerableApp {

    // 1. Inyección SQL clásica (Concatenación directa)
    public void searchUser(String username, Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        String sql = "SELECT * FROM users WHERE name = '" + username + "'";
        ResultSet rs = st.executeQuery(sql);
    }

    // 2. Path Traversal (Acceso a archivos sin sanitizar)
    public void downloadFile(String fileName) throws IOException {
        String base = "/app/data/files/";
        File file = new File(base + fileName);
        FileInputStream fis = new FileInputStream(file);
        // ... proceso de lectura
    }

    // 3. Command Injection (Ejecución de shell insegura)
    public void runDiagnostics(String host) throws IOException {
        String cmd = "ping -c 3 " + host;
        Runtime.getRuntime().exec(cmd);
    }

    // 4. Deserialización Insegura
    public void processData(byte[] bytes) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object obj = ois.readObject();
    }

    // 5. Hardcoded Credentials (Mala práctica)
    public void connectToDB() {
        String user = "admin";
        String pass = "SuperSecret123!";
        System.out.println("Connecting as " + user);
    }

    // 6. XSS (Cross Site Scripting) - Reflejado
    public void printWelcome(String name) {
        String html = "<h1>Welcome, " + name + "</h1>";
        System.out.println(html);
    }

    // 7. Uso de algoritmo criptográfico débil
    public String hashData(String input) {
        // MD5 es inseguro para contraseñas
        return java.security.MessageDigest.getInstance("MD5").toString();
    }

    // 8. Fuga de información en logs
    public void logError(Exception e) {
        System.err.println("Error grave: " + e.getMessage());
        e.printStackTrace();
    }
}