package ec.edu.espe.SecureFrameGallery.shared.utils;

import ec.edu.espe.SecureFrameGallery.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil — generación y validación de tokens JWT")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String email = "test@espe.edu.ec";
    private final String role = Role.ROLE_USER.name();

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
            "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm",
            3600000L
        );
    }

    // ── Generación ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Genera token no nulo para usuario válido")
    void generateTokenReturnsNonNullString() {
        String token = jwtUtil.generateToken(email, role);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Token tiene formato JWT de tres partes separadas por punto")
    void tokenHasThreeParts() {
        String token = jwtUtil.generateToken(email, role);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT debe tener header.payload.signature");
    }

    @Test
    @DisplayName("Dos tokens generados para el mismo usuario son distintos")
    void twoTokensForSameUserAreDifferent() {
        String token1 = jwtUtil.generateToken(email, role);
        String token2 = jwtUtil.generateToken(email, role);
        assertNotEquals(token1, token2, "Cada token debe ser único (ej. jti distinto)");
    }

    // ── Extracción de claims ──────────────────────────────────────────────────

    @Test
    @DisplayName("Extrae el email del subject correctamente")
    void extractsEmailFromSubject() {
        String token = jwtUtil.generateToken(email, role);
        String email = jwtUtil.extractEmail(token);
        assertEquals(this.email, email);
    }

    @Test
    @DisplayName("Extrae el rol del token correctamente")
    void extractsRoleFromToken() {
        String token = jwtUtil.generateToken(email, role);
        String role = jwtUtil.extractRole(token);
        assertEquals(this.role, role);
    }

    // ── Validación ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Token recién generado es válido")
    void freshTokenIsValid() {
        String token = jwtUtil.generateToken(email, role);
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    @DisplayName("Token firmado con secreto distinto es inválido")
    void tokenSignedWithDifferentSecretIsInvalid() {
        JwtUtil otherUtil = new JwtUtil(
            "completely-different-secret-key-256-bits-long-for-testing-purposes",
            3600000L
        );
        String foreignToken = otherUtil.generateToken(email, role);
        assertFalse(jwtUtil.isValid(foreignToken));
    }

    @Test
    @DisplayName("Token expirado es inválido")
    void expiredTokenIsInvalid() {
        JwtUtil shortLivedUtil = new JwtUtil(
            "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm",
            1L  // expira en 1ms
        );
        String token = shortLivedUtil.generateToken(email, role);

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertFalse(shortLivedUtil.isValid(token));
    }

    @Test
    @DisplayName("Token manipulado (payload alterado) es inválido")
    void tamperedTokenIsInvalid() {
        String token = jwtUtil.generateToken(email, role);
        String[] parts = token.split("\\.");
        // Reemplaza el payload con uno falso en base64
        String tamperedToken = parts[0] + ".dGFtcGVyZWQ" + "." + parts[2];
        assertFalse(jwtUtil.isValid(tamperedToken));
    }

    @Test
    @DisplayName("String vacío como token no lanza excepción")
    void emptyTokenDoesNotThrow() {
        assertDoesNotThrow(() -> assertFalse(jwtUtil.isValid("")));
    }

    @Test
    @DisplayName("Token de ROLE_SUPERVISOR contiene claim role correcto")
    void supervisorTokenContainsSupervisorRole() {
        String token = jwtUtil.generateToken(email, Role.ROLE_SUPERVISOR.name());
        assertEquals(Role.ROLE_SUPERVISOR.name(), jwtUtil.extractRole(token));
    }
}
