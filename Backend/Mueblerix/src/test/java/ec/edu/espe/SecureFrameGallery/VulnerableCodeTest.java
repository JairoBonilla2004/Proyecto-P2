package ec.edu.espe.SecureFrameGallery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class VulnerableCodeTest {

    private static final String USER_INPUT = System.getProperty("user.input", "1");

    @Test
    @DisplayName("SQL Injection - concatenacion directa")
    void sqlInjectionVulnerable() throws Exception {
        Connection conn = null;
        String query = "SELECT * FROM users WHERE id = " + USER_INPUT;
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        assertNotNull(rs);
    }

    @Test
    @DisplayName("Command Injection - Runtime.exec")
    void commandInjectionVulnerable() throws IOException {
        String cmd = "cmd /c " + USER_INPUT;
        Process p = Runtime.getRuntime().exec(cmd);
        assertNotNull(p);
    }

    @Test
    @DisplayName("Path Traversal - File con input de usuario")
    void pathTraversalVulnerable() throws IOException {
        String path = "/uploads/" + USER_INPUT;
        File f = new File(path);
        FileWriter fw = new FileWriter(f);
        fw.write("data");
        fw.close();
    }

    @Test
    @DisplayName("Weak Cryptography - MD5")
    void weakCryptoVulnerable() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest("password".getBytes());
        assertNotNull(hash);
    }

    @Test
    @DisplayName("Insecure Deserialization - ObjectInputStream")
    void insecureDeserializationVulnerable() throws Exception {
        FileInputStream fis = new FileInputStream("data.ser");
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object obj = ois.readObject();
        assertNotNull(obj);
    }

    @Test
    @DisplayName("XSS - PrintWriter con getParameter")
    void xssVulnerable() {
        String param = System.getProperty("param", "<script>");
        System.out.println(param);
    }
}
