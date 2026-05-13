package ec.edu.espe.SecureFrameGallery.modules.auth.services;

import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.LoginRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.RegisterRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.TokenResponse;
import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.auth.repositories.UserRepository;
import ec.edu.espe.SecureFrameGallery.modules.auth.services.impl.AuthServiceImpl;
import ec.edu.espe.SecureFrameGallery.shared.utils.Argon2Util;
import ec.edu.espe.SecureFrameGallery.shared.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

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
    private Argon2Util argon2Util;

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
        existingUser.setPasswordHash("hashed");
        existingUser.setRole(ec.edu.espe.SecureFrameGallery.shared.enums.Role.ROLE_USER);
        existingUser.setEnabled(true);
    }

    // ── Registro ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Registro exitoso retorna usuario guardado")
    void registerReturnsUser() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("nuevo");
        req.setEmail("nuevo@espe.edu.ec");
        req.setPassword("NuevoPass1!");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(argon2Util.hash(any())).thenReturn("hashed");

        User response = authService.register(req);

        assertNotNull(response);
        assertEquals(req.getEmail(), response.getEmail());
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

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
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

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
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
        when(argon2Util.verify(eq(req.getPassword()), eq(existingUser.getPasswordHash()))).thenReturn(true);
        when(jwtUtil.generateToken(eq(existingUser.getEmail()), eq(existingUser.getRole().name())))
            .thenReturn("mocked.jwt.token");

        TokenResponse response = authService.login(req);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
    }

    @Test
    @DisplayName("Login falla con contraseña incorrecta")
    void loginFailsWithWrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("juan@espe.edu.ec");
        req.setPassword("WrongPass1!");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(existingUser));
        when(argon2Util.verify(eq(req.getPassword()), eq(existingUser.getPasswordHash()))).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("Login falla con email inexistente — mensaje genérico")
    void loginFailsWithUnknownEmail() {
        LoginRequest req = new LoginRequest();
        req.setEmail("noexiste@espe.edu.ec");
        req.setPassword("Pass1234!");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        // Debe lanzar excepción con mensaje genérico (anti-enumeración)
        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("Login falla con cuenta deshabilitada")
    void loginFailsWithDisabledAccount() {
        existingUser.setEnabled(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("juan@espe.edu.ec");
        req.setPassword("Pass1234!");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(existingUser));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }
}
