package ec.edu.espe.Asistencia_con_Geofencing.controller;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.*;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ApiResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.AuthResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.UserResponse;
import ec.edu.espe.Asistencia_con_Geofencing.security.CustomUserDetails;
import ec.edu.espe.Asistencia_con_Geofencing.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para autenticación y registro de usuarios")
public class AuthController {

    private final AuthService authService;
    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario",
            description = "Crea una cuenta nueva con email, contraseña y rol (ESTUDIANTE por defecto)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o email duplicado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error del servidor")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterUserRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usuario registrado exitosamente", response));
    }


    @PostMapping("/authenticate")
    @Operation(summary = "Autenticar usuario",
            description = "Endpoint unificado para autenticar usando LOCAL (email/password), " +
                    "GOOGLE (idToken), FACEBOOK (accessToken), u otros proveedores OAuth. " +
                    "El campo 'provider' determina la estrategia de autenticación a usar. " +
                    "Si se proporciona fcmToken y deviceIdentifier, se registra el dispositivo automáticamente.",
            tags = {"Autenticación"})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Autenticación exitosa, JWT token retornado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o proveedor no soportado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error del servidor")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(ApiResponse.success("Autenticación exitosa", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión",
            description = "Desactiva el dispositivo actual para que deje de recibir notificaciones push. " +
                    "Solución profesional: cuando un usuario cierra sesión en un dispositivo prestado, " +
                    "ese dispositivo deja de recibir notificaciones de ese usuario. " +
                    "Si no se proporciona deviceIdentifier, se desactivan TODOS los dispositivos del usuario.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sesión cerrada exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ApiResponse<Object>> logout(
            @RequestBody(required = false) LogoutRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getId(), request != null ? request : new LogoutRequest());
        return ResponseEntity.ok(ApiResponse.success("Sesión cerrada exitosamente", null));
    }

}
