package ec.edu.espe.SecureFrameGallery.shared.utils;

import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil — generación y validación de tokens JWT")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
            "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm",
            3600000L
        );

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@espe.edu.ec");
        testUser.setUsername("tester");
        testUser.setRole(Role.USER);
        testUser.setEnabled(true);
    }

    // ── Generación ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Genera token no nulo para usuario válido")
    void generateTokenReturnsNonNullString() {
        String token = jwtUtil.generateToken(testUser);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Token tiene formato JWT de tres partes separadas por punto")
    void tokenHasThreeParts() {
        String token = jwtUtil.generateToken(testUser);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT debe tener header.payload.signature");
    }

    @Test
    @DisplayName("Dos tokens generados para el mismo usuario son distintos")
    void twoTokensForSameUserAreDifferent() {
        String token1 = jwtUtil.generateToken(testUser);
        String token2 = jwtUtil.generateToken(testUser);
        assertNotEquals(token1, token2, "Cada token debe tener iat distinto");
    }

    // ── Extracción de claims ──────────────────────────────────────────────────

    @Test
    @DisplayName("Extrae el email del subject correctamente")
    void extractsEmailFromSubject() {
        String token = jwtUtil.generateToken(testUser);
        String email = jwtUtil.extractEmail(token);
        assertEquals(testUser.getEmail(), email);
    }

    @Test
    @DisplayName("Extrae el rol del token correctamente")
    void extractsRoleFromToken() {
        String token = jwtUtil.generateToken(testUser);
        String role = jwtUtil.extractRole(token);
        assertEquals(Role.USER.name(), role);
    }

    // ── Validación ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Token recién generado es válido")
    void freshTokenIsValid() {
        String token = jwtUtil.generateToken(testUser);
        assertTrue(jwtUtil.isTokenValid(token, testUser));
    }

    @Test
    @DisplayName("Token firmado con secreto distinto es inválido")
    void tokenSignedWithDifferentSecretIsInvalid() {
        JwtUtil otherUtil = new JwtUtil(
            "completely-different-secret-key-256-bits-long-for-testing-purposes",
            3600000L
        );
        String foreignToken = otherUtil.generateToken(testUser);
        assertFalse(jwtUtil.isTokenValid(foreignToken, testUser));
    }

    @Test
    @DisplayName("Token expirado es inválido")
    void expiredTokenIsInvalid() {
        JwtUtil shortLivedUtil = new JwtUtil(
            "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm",
            1L  // expira en 1ms
        );
        String token = shortLivedUtil.generateToken(testUser);

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertFalse(shortLivedUtil.isTokenValid(token, testUser));
    }

    @Test
    @DisplayName("Token manipulado (payload alterado) es inválido")
    void tamperedTokenIsInvalid() {
        String token = jwtUtil.generateToken(testUser);
        String[] parts = token.split("\\.");
        // Reemplaza el payload con uno falso en base64
        String tamperedToken = parts[0] + ".dGFtcGVyZWQ" + "." + parts[2];
        assertFalse(jwtUtil.isTokenValid(tamperedToken, testUser));
    }

    @Test
    @DisplayName("String vacío como token no lanza excepción")
    void emptyTokenDoesNotThrow() {
        assertDoesNotThrow(() -> assertFalse(jwtUtil.isTokenValid("", testUser)));
    }

    @Test
    @DisplayName("Token para usuario distinto no valida contra otro usuario")
    void tokenDoesNotValidateForDifferentUser() {
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setEmail("otro@espe.edu.ec");
        otherUser.setUsername("otro");
        otherUser.setRole(Role.USER);
        otherUser.setEnabled(true);

        String tokenForOriginal = jwtUtil.generateToken(testUser);
        assertFalse(jwtUtil.isTokenValid(tokenForOriginal, otherUser));
    }

    // ── Claims de rol ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Token de SUPERVISOR contiene rol SUPERVISOR")
    void supervisorTokenContainsSupervisorRole() {
        testUser.setRole(Role.SUPERVISOR);
        String token = jwtUtil.generateToken(testUser);
        assertEquals(Role.SUPERVISOR.name(), jwtUtil.extractRole(token));
    }

    @Test
    @DisplayName("Token no expirado reporta isExpired como false")
    void freshTokenIsNotExpired() {
        String token = jwtUtil.generateToken(testUser);
        assertFalse(jwtUtil.isTokenExpired(token));
    }
}
