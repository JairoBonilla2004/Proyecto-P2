package ec.edu.espe.SecureFrameGallery.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Argon2Util — hashing y verificación de contraseñas")
class Argon2UtilTest {

    private final Argon2Util argon2Util = new Argon2Util();

    // ── Hash básico ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Hash no es igual a la contraseña en claro")
    void hashIsNotPlaintext() {
        String raw = "SecurePass@123";
        String hash = argon2Util.hash(raw);
        assertNotEquals(raw, hash);
    }

    @Test
    @DisplayName("Hash tiene longitud mayor a 20 caracteres")
    void hashHasMinimumLength() {
        String hash = argon2Util.hash("AnyPass1!");
        assertTrue(hash.length() > 20, "El hash debe tener longitud significativa");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Pass1!", "Contraseña@2024", "X9!abcDEF", "Sup3rS3cur3#"})
    @DisplayName("Distintas contraseñas producen hashes distintos entre sí")
    void differentPasswordsProduceDifferentHashes(String password) {
        String hash1 = argon2Util.hash("ReferencePass1!");
        String hash2 = argon2Util.hash(password);
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Hash comienza con identificador Argon2id")
    void hashStartsWithArgon2idPrefix() {
        String hash = argon2Util.hash("AnyPassword1!");
        assertTrue(hash.startsWith("$argon2id$"), "Debe usar Argon2id, no Argon2i ni Argon2d");
    }

    @Test
    @DisplayName("Verificación correcta con la misma contraseña")
    void verifyMatchesCorrectPassword() {
        String raw = "CorrectHorse#Battery9";
        String hash = argon2Util.hash(raw);
        assertTrue(argon2Util.verify(raw, hash));
    }

    @Test
    @DisplayName("Verificación falla con contraseña incorrecta")
    void verifyFailsWithWrongPassword() {
        String hash = argon2Util.hash("OriginalPass1!");
        assertFalse(argon2Util.verify("WrongPass1!", hash));
    }

    // ── Salt aleatorio ───────────────────────────────────────────────────────

    @RepeatedTest(5)
    @DisplayName("Dos hashes de la misma contraseña son distintos (salt aleatorio)")
    void differentHashesForSamePassword() {
        String raw = "SamePassword1!";
        String hash1 = argon2Util.hash(raw);
        String hash2 = argon2Util.hash(raw);
        assertNotEquals(hash1, hash2, "El salt debe ser aleatorio — los hashes no deben coincidir");
    }

    // ── Casos borde ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Contraseña muy larga no rompe el hash")
    void veryLongPasswordDoesNotThrow() {
        String longPass = "A1!".repeat(100);
        assertDoesNotThrow(() -> {
            String hash = argon2Util.hash(longPass);
            assertTrue(argon2Util.verify(longPass, hash));
        });
    }

    @Test
    @DisplayName("Contraseña con caracteres Unicode se hashea correctamente")
    void unicodePasswordHashesCorrectly() {
        String unicodePass = "Pässwörð@123";
        String hash = argon2Util.hash(unicodePass);
        assertTrue(argon2Util.verify(unicodePass, hash));
        assertFalse(argon2Util.verify("Passwor@123", hash));
    }

    @Test
    @DisplayName("Hash vacío retorna false en verificación")
    void emptyHashReturnsFalse() {
        assertFalse(argon2Util.verify("anypassword", ""));
    }

    @Test
    @DisplayName("Hash corrupto retorna false sin lanzar excepción")
    void corruptHashDoesNotThrow() {
        assertDoesNotThrow(() ->
            assertFalse(argon2Util.verify("pass", "$argon2id$corrupted_garbage_hash"))
        );
    }

    // ── Seguridad: timing attack ─────────────────────────────────────────────

    @Test
    @DisplayName("Verificación de hash inválido no es significativamente más rápida que una válida")
    void verificationTimingIsSimilarForValidAndInvalidHash() {
        String hash = argon2Util.hash("TestPass1!");

        long start1 = System.nanoTime();
        argon2Util.verify("WrongPass1!", hash);
        long elapsed1 = System.nanoTime() - start1;

        long start2 = System.nanoTime();
        argon2Util.verify("TestPass1!", hash);
        long elapsed2 = System.nanoTime() - start2;

        double ratio = (double) Math.max(elapsed1, elapsed2) / Math.min(elapsed1, elapsed2);
        assertTrue(ratio < 100, "Posible timing oracle: ratio de tiempos = " + ratio);
    }

    // ── Formato del hash ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Hash contiene parámetros de memoria e iteraciones en su formato")
    void hashContainsMemoryAndIterationParams() {
        String hash = argon2Util.hash("ParamCheck1!");
        assertTrue(hash.contains("m=") || hash.contains("t="),
            "El hash Argon2id debe contener parámetros de configuración");
    }

    @Test
    @DisplayName("Verificación de contraseña vacía contra hash real retorna false")
    void emptyPasswordVerificationReturnsFalse() {
        String hash = argon2Util.hash("RealPass1!");
        assertFalse(argon2Util.verify("", hash));
    }
}
