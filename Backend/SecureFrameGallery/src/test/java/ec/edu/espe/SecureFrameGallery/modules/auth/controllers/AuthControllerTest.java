package ec.edu.espe.SecureFrameGallery.modules.auth.controllers;

import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.LoginRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.RegisterRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.TokenResponse;
import ec.edu.espe.SecureFrameGallery.modules.auth.services.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — endpoints de autenticación")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    // ── Registro ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register retorna 201 con token")
    void registerReturns201WithToken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("nuevo_usuario");
        req.setEmail("nuevo@espe.edu.ec");
        req.setPassword("Seguro1234!");

        TokenResponse token = new TokenResponse("jwt.token.aqui");
        when(authService.register(any(RegisterRequest.class))).thenReturn(token);

        ResponseEntity<?> response = authController.register(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("POST /auth/register propaga excepción si email duplicado")
    void registerPropagatesExceptionOnDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("otro");
        req.setEmail("duplicado@espe.edu.ec");
        req.setPassword("Pass1234!");

        when(authService.register(any())).thenThrow(new RuntimeException("Email ya registrado"));

        assertThrows(RuntimeException.class, () -> authController.register(req));
    }

    @Test
    @DisplayName("POST /auth/register propaga excepción si username duplicado")
    void registerPropagatesExceptionOnDuplicateUsername() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("existente");
        req.setEmail("nuevo2@espe.edu.ec");
        req.setPassword("Pass1234!");

        when(authService.register(any())).thenThrow(new RuntimeException("Username no disponible"));

        assertThrows(RuntimeException.class, () -> authController.register(req));
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login retorna 200 con token")
    void loginReturns200WithToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@espe.edu.ec");
        req.setPassword("Pass1234!");

        TokenResponse token = new TokenResponse("jwt.token.valido");
        when(authService.login(any(LoginRequest.class))).thenReturn(token);

        ResponseEntity<?> response = authController.login(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("POST /auth/login propaga excepción con credenciales inválidas")
    void loginPropagatesExceptionWithInvalidCredentials() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@espe.edu.ec");
        req.setPassword("Incorrecta1!");

        when(authService.login(any())).thenThrow(new RuntimeException("Credenciales inválidas"));

        assertThrows(RuntimeException.class, () -> authController.login(req));
    }

    @Test
    @DisplayName("POST /auth/login mensaje de error no revela si usuario existe")
    void loginErrorMessageDoesNotRevealUserExistence() {
        LoginRequest req = new LoginRequest();
        req.setEmail("noexiste@espe.edu.ec");
        req.setPassword("Pass1234!");

        when(authService.login(any())).thenThrow(new RuntimeException("Credenciales inválidas"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authController.login(req));
        assertFalse(ex.getMessage().toLowerCase().contains("no encontrado"),
            "El mensaje no debe revelar si el email existe en el sistema");
    }

    // ── Token response ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Token response contiene el token JWT en el cuerpo")
    void responseBodyContainsToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@espe.edu.ec");
        req.setPassword("Pass1234!");

        String expectedToken = "eyJhbGciOiJIUzI1NiJ9.payload.signature";
        when(authService.login(any())).thenReturn(new TokenResponse(expectedToken));

        ResponseEntity<?> response = authController.login(req);

        assertTrue(response.getBody().toString().contains("token") ||
                   response.getBody() instanceof TokenResponse,
            "El cuerpo debe contener el token JWT");
    }

    @Test
    @DisplayName("Registro invoca authService exactamente una vez")
    void registerInvokesServiceOnce() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("unico");
        req.setEmail("unico@espe.edu.ec");
        req.setPassword("Pass1234!");

        when(authService.register(any())).thenReturn(new TokenResponse("token"));

        authController.register(req);

        verify(authService, times(1)).register(any());
    }
}

