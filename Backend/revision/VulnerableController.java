package com.vulnerable.controller;

import com.vulnerable.model.User;
import com.vulnerable.service.VulnerableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api")
public class VulnerableController {

    @Autowired
    private VulnerableService vulnerableService;

    // ============================================================
    // A05:2025 - SQL Injection
    // ============================================================
    @GetMapping("/users/search")
    public List<User> searchUsers(@RequestParam String q) {
        // Vulnerable: entrada del usuario usada directamente en SQL
        return vulnerableService.searchUsers(q);
    }

    @GetMapping("/users/role")
    public List<User> getUsersByRole(@RequestParam String role) {
        return vulnerableService.findByRole(role);
    }

    @DeleteMapping("/user/{id}")
    public String deleteUser(@PathVariable String id) {
        // A01: Sin autenticación ni autorización
        vulnerableService.deleteUser(id);
        return "Usuario eliminado";
    }

    // ============================================================
    // A01:2025 - Broken Access Control (IDOR)
    // ============================================================
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        // A01: No verifica que el usuario tenga permiso para ver este dato
        // A05: Vulnerable a SQL Injection
        return vulnerableService.findByUsername(id.toString());
    }

    // ============================================================
    // A05:2025 - Command Injection
    // ============================================================
    @GetMapping("/ping")
    public String ping(@RequestParam String host) {
        return vulnerableService.pingHost(host);
    }

    // ============================================================
    // A01:2025 - Path Traversal
    // ============================================================
    @GetMapping("/file")
    public String readFile(@RequestParam String filename) {
        return vulnerableService.readFile(filename);
    }

    // ============================================================
    // A01:2025 - SSRF
    // ============================================================
    @GetMapping("/fetch")
    public String fetchUrl(@RequestParam String url) {
        return vulnerableService.fetchUrl(url);
    }

    // ============================================================
    // A08:2025 - Insecure Deserialization
    // ============================================================
    @PostMapping("/deserialize")
    public Object deserialize(@RequestBody String base64Data) {
        byte[] data = Base64.getDecoder().decode(base64Data);
        return vulnerableService.deserialize(data);
    }

    // ============================================================
    // A04:2025 - Cryptographic Failures + A09: Logging Failures
    // ============================================================
    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        // A06: Mass Assignment - permite establecer el rol desde el cliente
        // A09: Log de datos sensibles
        return vulnerableService.createUser(user);
    }

    // ============================================================
    // A07:2025 - Authentication Failures (Autenticación débil)
    // ============================================================
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        // Vulnerable: autenticación sin hash, sin sesión segura
        User user = vulnerableService.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return "Login exitoso. Bienvenido " + username;
        }
        return "Credenciales inválidas";
    }

    // ============================================================
    // A07:2025 - Hardcoded Credentials
    // ============================================================
    @PostMapping("/admin/login")
    public String adminLogin(@RequestParam String username, @RequestParam String password) {
        // A07: Credenciales hardcodeadas en el código
        if ("admin".equals(username) && "admin123".equals(password)) {
            return "Acceso de administrador concedido";
        }
        return "Acceso denegado";
    }

    // ============================================================
    // A02:2025 - Security Misconfiguration (expone configuración)
    // ============================================================
    @GetMapping("/config")
    public String getConfig() {
        // Expone información sensible de configuración
        return "DB: jdbc:h2:mem:testdb, User: sa, Password: (empty), " +
               "Debug: enabled, Actuator: exposed";
    }

    // ============================================================
    // A10:2025 - Mishandling of Exceptional Conditions
    // ============================================================
    @GetMapping("/divide")
    public String divide(@RequestParam int a, @RequestParam int b) {
        try {
            return "Resultado: " + (a / b);
        } catch (Exception e) {
            // A10: Expone traza completa del error
            return "Error: " + e.toString() + "\n" + 
                   java.util.Arrays.toString(e.getStackTrace());
        }
    }

    // ============================================================
    // A06:2025 - Insecure Design (Reset de contraseña sin verificación)
    // ============================================================
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String username, @RequestParam String newPassword) {
        // A06: No verifica la identidad del usuario
        User user = vulnerableService.findByUsername(username);
        if (user != null) {
            user.setPassword(newPassword);
            vulnerableService.createUser(user);
            return "Contraseña actualizada para: " + username;
        }
        return "Usuario no encontrado";
    }

    // ============================================================
    // A03:2025 - Software Supply Chain Failures (usando librerías vulnerables)
    // ============================================================
    @GetMapping("/greet")
    public String greet(@RequestParam String name) {
        // Usa commons-text (vulnerable a Text4Shell)
        org.apache.commons.text.StringEscapeUtils.escapeHtml4(name);
        return "Hola, " + name;
    }
}