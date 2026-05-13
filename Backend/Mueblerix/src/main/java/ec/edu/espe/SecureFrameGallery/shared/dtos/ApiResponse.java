package ec.edu.espe.SecureFrameGallery.shared.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

/**
 * Envoltorio genérico para todas las respuestas de la API.
 * Principio de Responsabilidad Única (SRP): solo encapsula la estructura de respuesta.
 *
 * Estructura:
 * {
 *   "success": true/false,
 *   "message": "Mensaje principal para el usuario",
 *   "details": "Detalles adicionales o técnicos (opcional)",
 *   "errorCode": 400|401|403|404|500 (solo en errores),
 *   "timestamp": "2024-05-03T12:00:00Z",
 *   "data": { ... } (solo en respuestas exitosas)
 * }
 *
 * @param <T> tipo del payload de datos
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final String details;          // Detalles adicionales (ej: campo específico en validación)
    private final Integer errorCode;       // Código HTTP para debugging
    private final T data;
    private final Instant timestamp;

    private ApiResponse(boolean success, String message, String details, Integer errorCode, T data) {
        this.success = success;
        this.message = message;
        this.details = details;
        this.errorCode = errorCode;
        this.data = data;
        this.timestamp = Instant.now();
    }

    /**
     * Respuesta de éxito con datos.
     * Ejemplo: { "success": true, "message": "OK", "data": { ... }, "timestamp": "..." }
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", null, null, data);
    }

    /**
     * Respuesta de éxito con mensaje personalizado y datos.
     * Ejemplo: { "success": true, "message": "Usuario creado", "data": {...}, "timestamp": "..." }
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, null, null, data);
    }

    /**
     * Respuesta de error simple.
     * Ejemplo: { "success": false, "message": "Error al procesar", "timestamp": "..." }
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null, null);
    }

    /**
     * Respuesta de error con detalles.
     * Usado cuando necesitas mostrar más información al usuario o desarrollador.
     * Ejemplo: { "success": false, "message": "Validación fallida", "details": "email: formato inválido", "timestamp": "..." }
     */
    public static <T> ApiResponse<T> error(String message, String details) {
        return new ApiResponse<>(false, message, details, null, null);
    }

    /**
     * Respuesta de error con detalles y código HTTP.
     * Usado para errores que necesitan contexto técnico (debugging).
     * Ejemplo: { "success": false, "message": "No encontrado", "details": "El album con ID xxx no existe", "errorCode": 404, "timestamp": "..." }
     */
    public static <T> ApiResponse<T> error(String message, String details, Integer errorCode) {
        return new ApiResponse<>(false, message, details, errorCode, null);
    }
}
