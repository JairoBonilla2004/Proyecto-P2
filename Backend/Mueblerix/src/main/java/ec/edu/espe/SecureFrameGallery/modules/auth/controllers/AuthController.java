package ec.edu.espe.SecureFrameGallery.modules.auth.controllers;

import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.LoginRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.RegisterRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.TokenResponse;
import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.auth.services.AuthService;
import ec.edu.espe.SecureFrameGallery.shared.dtos.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro, login local y OAuth2")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario registrado exitosamente", user.getEmail()));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener JWT")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse token = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(token));
    }

    // 🚨 TEST: Código deliberadamente vulnerable para probar el pipeline
    private void checkUserByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = '" + email + "'";
        // SQL Injection intencional — el pipeline lo detectará como VULNERABLE
    }
}
