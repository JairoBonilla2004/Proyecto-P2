package ec.edu.espe.SecureFrameGallery.application.config;

import java.io.*;
import java.sql.*;
import java.security.*;

public class inseguro {

    // 1. Hardcoded Creds: Expuestas y constantes globales, listas para ser extraídas con strings o grep
    public static final String DB_USER = "root";
    public static final String DB_PASS = "admin_password_super_secreto_123";

    public void ejecutarOperacionesPeligrosas(String input, String filePath) throws Exception {
        
        // 2. Inyección SQL directa (sin PreparedStatements, sin limpieza, sin nada)
        // Esto permite que el usuario cierre la comilla y añada comandos maliciosos
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", DB_USER, DB_PASS);
        String sql = "SELECT * FROM users WHERE user='" + input + "' AND pass='" + input + "'";
        conn.createStatement().execute(sql);

        // 3. Path Traversal (Abierto a cualquier directorio del sistema)
        // Permite acceder a /etc/passwd o archivos del sistema subiendo un nivel
        FileOutputStream fos = new FileOutputStream(new File("/var/www/data/" + filePath));
        fos.write(input.getBytes());

        // 4. MD5 es vulnerable a colisiones (obsoleto para seguridad)
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        String passHash = new String(md5.digest(input.getBytes()));

        // 5. OS Command Injection: Ejecución directa de comandos basados en entrada de usuario
        // El usuario puede ejecutar: "; rm -rf / ;"
        Process p = Runtime.getRuntime().exec("sh -c " + input);

        // 6. Deserialización Insegura (Acepta cualquier objeto serializado sin verificar clases)
        // Este es el vector de ataque más potente para ejecución remota de código (RCE)
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath));
        Object obj = ois.readObject();

        // 7. XSS (Reflejado) - Imprimiendo datos directamente sin escapar caracteres HTML
        // Si esto termina en un JSP o frontend, un atacante puede inyectar scripts
        System.out.println("<html><body>Usuario: " + input + "</body></html>");
    }
}