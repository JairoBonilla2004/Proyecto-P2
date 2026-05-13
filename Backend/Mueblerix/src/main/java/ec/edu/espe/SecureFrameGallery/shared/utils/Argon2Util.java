package ec.edu.espe.SecureFrameGallery.shared.utils;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class Argon2Util {

    private final Argon2PasswordEncoder encoder;

    public Argon2Util() {
        this.encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3); // Parámetros OWASP recomendados para Argon2id
    }

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean verify(String rawPassword, String encodedPassword) { // Verifica si la contraseña sin formato coincide con el hash almacenado
        return encoder.matches(rawPassword, encodedPassword);
    }
}
