package ec.edu.espe.Asistencia_con_Geofencing;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class para generar hashes BCrypt de contraseñas
 * Ejecutar: java PasswordHashGenerator
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generar hash para contraseña "admin123"
        String password = "admin123";
        String hash = encoder.encode(password);
        
        System.out.println("===========================================");
        System.out.println("Password Hash Generator");
        System.out.println("===========================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash:");
        System.out.println(hash);
        System.out.println("===========================================");
        System.out.println("\nUsar este hash en el script SQL:");
        System.out.println("INSERT INTO users (id, email, password, full_name, provider, enabled, created_at)");
        System.out.println("VALUES (gen_random_uuid(), 'admin@espe.edu.ec', '" + hash + "', 'Administrador Sistema', 'LOCAL', true, CURRENT_TIMESTAMP);");
        System.out.println("===========================================");
    }
}
