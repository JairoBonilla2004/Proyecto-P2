package ec.edu.espe.SecureFrameGallery.application.exceptions;

import ec.edu.espe.SecureFrameGallery.shared.dtos.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.UUID;
import java.util.stream.Collectors;


@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationError(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validación fallida en request - {}", fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                    "Los datos enviados no cumplen con los requisitos",
                    "Errores de validación: " + fieldErrors,
                    HttpStatus.BAD_REQUEST.value()
                ));
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        
        log.warn("Argumento inválido - {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                    "Parámetro inválido",
                    ex.getMessage(),
                    HttpStatus.BAD_REQUEST.value()
                ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, WebRequest request) {
        
        log.warn("Intento de carga de archivo excediendo límite de tamaño");
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(
                    "Archivo demasiado grande",
                    "El tamaño máximo permitido es 10MB. Por favor, elige un archivo más pequeño.",
                    HttpStatus.PAYLOAD_TOO_LARGE.value()
                ));
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientAuthentication(
            InsufficientAuthenticationException ex, WebRequest request) {
        
        log.warn("Acceso denegado - Autenticación insuficiente");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                    "No estás autenticado",
                    "Se requiere un token JWT válido en el header Authorization. Formato: Bearer <token>",
                    HttpStatus.UNAUTHORIZED.value()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        log.warn("Error de autenticación - {}", ex.getMessage());
        
        String userMessage = "No pudimos validar tu identidad";
        
        if (ex instanceof BadCredentialsException) {
            userMessage = "Email o contraseña incorrectos";
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                    userMessage,
                    "Por favor, intenta iniciar sesión nuevamente con credenciales válidas",
                    HttpStatus.UNAUTHORIZED.value()
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        
        log.warn("Intento de login con credenciales inválidas");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                    "Credenciales inválidas",
                    "Email o contraseña incorrectos. Por favor, verifica e intenta nuevamente.",
                    HttpStatus.UNAUTHORIZED.value()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        
        log.warn("Acceso denegado - Usuario sin permisos suficientes");
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                    "No tienes permiso para esta operación",
                    "Tu rol no te permite acceder a este recurso. Contacta al administrador si crees que es un error.",
                    HttpStatus.FORBIDDEN.value()
                ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, WebRequest request) {
        log.warn("Error de negocio [{}] - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(
                    ex.getMessage(),
                    "La operación no puede completarse debido a restricciones de negocio. " +
                    "Código de error: " + ex.getErrorCode(),
                    ex.getHttpStatus().value()
                ));
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidState(
            InvalidStateException ex, WebRequest request) {
        
        log.warn("Estado inválido [{}] - {}", ex.getErrorCode(), ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                    "Cambio de estado no permitido",
                    ex.getMessage(),
                    HttpStatus.CONFLICT.value()
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        
        log.warn("Recurso no encontrado - {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                    "Recurso no encontrado",
                    ex.getMessage(),
                    HttpStatus.NOT_FOUND.value()
                ));
    }


    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException ex, WebRequest request) {
        
        log.warn("Endpoint no encontrado - {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                    "Endpoint no encontrado",
                    "La ruta solicitada no existe en esta API",
                    HttpStatus.NOT_FOUND.value()
                ));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(
            EntityNotFoundException ex, WebRequest request) {
        
        log.warn("Entidad no encontrada en base de datos - {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                    "El recurso que buscas no existe",
                    ex.getMessage(),
                    HttpStatus.NOT_FOUND.value()
                ));
    }

    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ApiResponse<Void>> handleIOException(
            java.io.IOException ex, WebRequest request) {
        
        String errorId = generateErrorId();
        log.error("Error de I/O [ID: {}] - {}", errorId, ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                    "Error al procesar el archivo",
                    "Hubo un problema al leer o guardar el archivo. ID de error: " + errorId,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {
        
        String errorId = generateErrorId();
        log.error("Excepción no capturada [ID: {}] - Tipo: {} - Mensaje: {}",
            errorId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        
        // En producción, ocultamos el tipo exacto de excepción por seguridad
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                    "Error interno del servidor",
                    "Algo salió mal procesando tu solicitud. Reporta este error ID: " + errorId,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                ));
    }

    private String generateErrorId() {
        return "ERR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

