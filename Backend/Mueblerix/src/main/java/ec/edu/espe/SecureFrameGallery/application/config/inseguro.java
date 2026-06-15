package ec.edu.espe.SecureFrameGallery.application.config;

import java.io.*;
import java.sql.*;
import java.security.*;
import javax.xml.parsers.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class inseguro {

    // 1. HARDCODED SECRETS: Las llaves del reino en texto plano
    private static final String API_KEY = "chirindsaaoo_aaa"; 
    private static final String DB_PASS = "admin123";

    public void procesarSolicitud(String userInput, String fileName) throws Exception {
        
        // 2. SQL INJECTION MASIVA: Uso de concatenación y ejecución múltiple
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db?allowMultiQueries=true", "root", DB_PASS);
        Statement st = conn.createStatement();
        // Permite borrar tablas o extraer toda la base con un simple input
        st.execute("UPDATE users SET password='" + userInput + "' WHERE id='" + userInput + "'; DROP TABLE logs;");

        // 3. PATH TRAVERSAL: Sin saneamiento de entrada
        // Permite acceder a /etc/passwd o archivos críticos del sistema
        File file = new File("/var/www/uploads/" + userInput);
        BufferedReader br = new BufferedReader(new FileReader(file));

        // 4. COMMAND INJECTION: Ejecución de comandos del sistema
        // El usuario puede ejecutar "; rm -rf /"
        Runtime.getRuntime().exec("ping -c 3 " + userInput);

        // 5. INSECURE DESERIALIZATION: RCE (Remote Code Execution) garantizado
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
        Object obj = ois.readObject();

        // 6. XXE (XML External Entity): Lectura de archivos internos vía XML
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        dbf.parse(new ByteArrayInputStream(userInput.getBytes()));

        // 7. WEAK CRYPTO: Algoritmos obsoletos
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding"); // DES es extremadamente débil
        SecretKey key = new SecretKeySpec("12345678".getBytes(), "DES");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        // 8. LOGGING SENSITIVE DATA: Escribir secretos en los logs
        System.out.println("Procesando solicitud con API_KEY: " + API_KEY);
    }
}