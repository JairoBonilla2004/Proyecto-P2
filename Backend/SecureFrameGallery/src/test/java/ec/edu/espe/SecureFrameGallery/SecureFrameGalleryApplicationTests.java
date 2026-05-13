package ec.edu.espe.SecureFrameGallery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de integración: verifica que el contexto de Spring Boot carga correctamente.
 * Usa el perfil "test" con H2 en memoria para evitar dependencias externas.
 */
@SpringBootTest
@ActiveProfiles("test")
class SecureFrameGalleryApplicationTests {

	@Test
	void contextLoads() {
		// Si Spring Boot arranca sin excepciones, la prueba pasa.
		// Valida: configuración de seguridad, JPA, beans de servicio, etc.
	}
}
