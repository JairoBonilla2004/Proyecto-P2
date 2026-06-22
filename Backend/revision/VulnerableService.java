package com.vulnerable.service;

import com.vulnerable.model.User;
import com.vulnerable.repository.UserRepository;
import org.apache.commons.text.StringEscapeUtils;  // A03: Text4Shell
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;  // A04: MD5 inseguro
import java.util.List;

@Service
public class VulnerableService {

    private static final Logger logger = LoggerFactory.getLogger(VulnerableService.class);

    @Autowired
    private UserRepository userRepository;

    // ============================================================
    // A04:2025 - Cryptographic Failures (Hash MD5 inseguro)
    // ============================================================
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return password;  // A10: Fallo silencioso - devuelve texto plano
        }
    }

    // ============================================================
    // A05:2025 - Command Injection
    // ============================================================
    public String pingHost(String host) {
        try {
            // Vulnerable: ejecuta comandos del sistema con entrada del usuario
            Process process = Runtime.getRuntime().exec("ping -c 4 " + host);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        } catch (IOException e) {
            // A10: Expone traza completa del error
            return "Error: " + e.toString();
        }
    }

    // ============================================================
    // A01:2025 - Broken Access Control + Path Traversal
    // ============================================================
    public String readFile(String filename) {
        try {
            // Vulnerable: Path Traversal - sin validación de ruta
            File file = new File("/tmp/" + filename);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    // ============================================================
    // A01:2025 - SSRF (Server-Side Request Forgery)
    // ============================================================
    public String fetchUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            return "Error fetching URL: " + e.getMessage();
        }
    }

    // ============================================================
    // A08:2025 - Insecure Deserialization
    // ============================================================
    public Object deserialize(byte[] data) {
        try {
            // Vulnerable: deserializa datos sin validación
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bis);
            return ois.readObject();
        } catch (Exception e) {
            return "Deserialization error: " + e.getMessage();
        }
    }

    // ============================================================
    // A09:2025 - Logging Failures (log de datos sensibles)
    // ============================================================
    public User createUser(User user) {
        // A09: Log de contraseña en texto plano
        logger.info("Creando usuario: {} con contraseña: {}", 
            user.getUsername(), user.getPassword());
        userRepository.saveUser(user);
        return user;
    }

    // ============================================================
    // Métodos de acceso a datos (con SQL Injection)
    // ============================================================
    public List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }

    public List<User> findByRole(String role) {
        return userRepository.findByRole(role);
    }

    public void deleteUser(String userId) {
        userRepository.deleteUser(userId);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}