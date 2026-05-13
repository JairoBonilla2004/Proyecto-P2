package ec.edu.espe.SecureFrameGallery.modules.auth.services;

import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.LoginRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.RegisterRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.TokenResponse;
import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.auth.repositories.UserRepository;
import ec.edu.espe.SecureFrameGallery.modules.auth.services.impl.AuthServiceImpl;
import ec.edu.espe.SecureFrameGallery.shared.enums.Role;
import ec.edu.espe.SecureFrameGallery.shared.utils.Argon2Util;
import ec.edu.espe.SecureFrameGallery.shared.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl — registro y login")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setUsername("juanito");
        existingUser.setEmail("juan@espe.edu.ec");
        existingUser.setPasswordHash(Argon2Util.hash("Pass1234!"));
        existingUser.setRole(Role.USER);
        existingUser.setEnabled(true);
    }

    // ── Registro ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Registro exitoso retorna token")
    void registerReturnsToken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("nuevo");
        req.setEmail("nuevo@espe.edu.ec");
        req.setPassword("NuevoPass1!");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(any())).thenReturn("mocked.jwt.token");

        TokenResponse response = authService.register(req);

        assertNotNull(response);
        assertNotNull(response.getToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Registro falla si el email ya existe")
    void registerFailsIfEmailTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("otro");
        req.setEmail("juan@espe.edu.ec");
        req.setPassword("Pass1234!");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registro falla si el username ya existe")
    void registerFailsIfUsernameTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("juanito");
        req.setEmail("otro@espe.edu.ec");
        req.setPassword("Pass1234!");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(req.getUsername())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login exitoso retorna token")
    void loginReturnsToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("juan@espe.edu.ec");
        req.setPassword("Pass1234!");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateToken(any())).thenReturn("mocked.jwt.token");

        TokenResponse response = authService.login(req);

        assertNotNull(response);
        assertNotNull(response.getToken());
    }

    @Test
    @DisplayName("Login falla con contraseña incorrecta")
    void loginFailsWithWrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("juan@espe.edu.ec");
        req.setPassword("WrongPass1!");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(existingUser));

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("Login falla con email inexistente — mensaje genérico")
    void loginFailsWithUnknownEmail() {
        LoginRequest req = new LoginRequest();
        req.setEmail("noexiste@espe.edu.ec");
        req.setPassword("Pass1234!");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        // Debe lanzar excepción con mensaje genérico (anti-enumeración)
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req));
        assertFalse(ex.getMessage().toLowerCase().contains("usuario"),
            "El mensaje no debe revelar si el usuario existe");
    }

    @Test
    @DisplayName("Login falla con cuenta deshabilitada")
    void loginFailsWithDisabledAccount() {
        existingUser.setEnabled(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("juan@espe.edu.ec");
        req.setPassword("Pass1234!");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(existingUser));

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }
}
