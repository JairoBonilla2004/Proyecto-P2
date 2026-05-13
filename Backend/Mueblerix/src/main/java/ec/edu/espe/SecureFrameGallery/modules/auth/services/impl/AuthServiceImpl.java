package ec.edu.espe.SecureFrameGallery.modules.auth.services.impl;

import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.LoginRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.RegisterRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.TokenResponse;
import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.auth.repositories.UserRepository;
import ec.edu.espe.SecureFrameGallery.modules.auth.services.AuthService;
import ec.edu.espe.SecureFrameGallery.shared.enums.AuthProvider;
import ec.edu.espe.SecureFrameGallery.shared.enums.Role;
import ec.edu.espe.SecureFrameGallery.shared.utils.Argon2Util;
import ec.edu.espe.SecureFrameGallery.shared.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final Argon2Util argon2Util;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Override
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El correo o nombre de usuario ya está en uso");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El correo o nombre de usuario ya está en uso");
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(argon2Util.hash(request.getPassword()))
                .role(Role.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .build();

        User saved = userRepository.save(newUser);
        log.info("Usuario registrado: {}", saved.getEmail());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (user.getPasswordHash() == null || !argon2Util.verify(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        log.info("Login exitoso: {}", user.getEmail());

        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expirationMs / 1000)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public TokenResponse processOAuth2Login(String email, String name) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User oauth2User = User.builder()
                    .username(sanitizeUsername(name))
                    .email(email)
                    .role(Role.ROLE_USER)
                    .provider(AuthProvider.GOOGLE)
                    .enabled(true)
                    .build();
            User saved = userRepository.save(oauth2User);
            log.info("Nuevo usuario OAuth2 creado: {} (provider: GOOGLE)", email);
            return saved;
        });
        
        if (!user.isEnabled()) {
            log.warn("Intento de login con usuario deshabilitado: {}", email);
            throw new IllegalArgumentException("Usuario deshabilitado");
        }
        
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        log.info("Login OAuth2 (Google) exitoso: {}", email);

        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expirationMs / 1000)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private String sanitizeUsername(String name) { // Convierte el nombre a un formato seguro para el username
        if (name == null || name.isBlank()) return "user_" + System.currentTimeMillis();
        String sanitized = name.replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase();
        if (userRepository.existsByUsername(sanitized)) {
            sanitized = sanitized + "_" + System.currentTimeMillis();
        }
        return sanitized;
    }
}
