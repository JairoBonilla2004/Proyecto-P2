package ec.edu.espe.SecureFrameGallery.application.config;

import java.io.*;
import java.sql.*;

public class inseguro {

    // No requiere ninguna dependencia extra, solo el JDK
    private static final String DB_URL = "jdbc:mysql://localhost:3306/db";

    public void ejecutarOperaciones(String input) throws Exception {
        
        // 1. SQL Injection: Directo con java.sql.Statement
        Connection conn = DriverManager.getConnection(DB_URL, "root", "admin123");
        Statement st = conn.createStatement();
        // El modelo de IA detectará la concatenación de strings en una consulta SQL
        st.execute("SELECT * FROM users WHERE name = '" + input + "'");

        // 2. Command Injection: Directo con java.lang.Runtime
        // El modelo detectará el uso de exec() con entrada de usuario
        Runtime.getRuntime().exec("cmd.exe /c dir " + input);

        // 3. Path Traversal: Directo con java.io.FileReader
        // El modelo detectará la construcción de rutas con variables externas
        FileReader fr = new FileReader("/app/data/" + input);
        int ch;
        while ((ch = fr.read()) != -1) {
            System.out.print((char) ch);
        }
    }
}