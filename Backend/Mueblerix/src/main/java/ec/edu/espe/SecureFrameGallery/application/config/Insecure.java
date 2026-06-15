package ec.edu.espe.SecureFrameGallery.application.config;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.io.File;
import java.io.FileOutputStream;

public class Insecure {

    // VULNERABILIDAD 1: SQL Injection
    // La consulta se construye concatenando strings directamente desde el usuario.
    public void getUserData(String username, Connection conn) throws Exception {
        Statement stmt = conn.createStatement();
        String query = "SELECT * FROM users WHERE username = '" + username + "'";
        ResultSet rs = stmt.executeQuery(query);
        // ... procesar resultados
    }

    // VULNERABILIDAD 2: Path Traversal
    // Permite escribir archivos en directorios arbitrarios si el input no es validado.
    public void saveUserFile(String filename, byte[] data) throws Exception {
        String baseDir = "/var/www/uploads/";
        File file = new File(baseDir + filename); // Peligroso: "../../etc/passwd"
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
    }

    // VULNERABILIDAD 3: Ejecución de comandos del sistema
    // Uso inseguro de Runtime.exec con concatenación de parámetros.
    public void pingServer(String ipAddress) throws Exception {
        String command = "ping -c 4 " + ipAddress; 
        // Si el usuario ingresa: "127.0.0.1; rm -rf /", se ejecuta el comando malicioso.
        Runtime.getRuntime().exec(command);
    }
}