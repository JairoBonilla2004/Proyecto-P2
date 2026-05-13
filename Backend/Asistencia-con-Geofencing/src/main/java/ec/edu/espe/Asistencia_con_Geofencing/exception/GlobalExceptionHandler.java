package ec.edu.espe.Asistencia_con_Geofencing.exception;

import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), "INVALID_CREDENTIALS"));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleTokenExpired(TokenExpiredException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "TOKEN_EXPIRED"));
    }

    @ExceptionHandler(OutsideGeofenceException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleOutsideGeofence(OutsideGeofenceException ex) {
        Map<String, Object> data = new HashMap<>();
        data.put("requiredZone", ex.getRequiredZone());
        data.put("distance", ex.getDistance());
        data.put("maxRadius", ex.getMaxRadius());

        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                false,
                ex.getMessage(),
                data,
                "OUTSIDE_GEOFENCE",
                java.time.LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(AlreadyRegisteredException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAlreadyRegistered(AlreadyRegisteredException ex) {
        Map<String, Object> data = new HashMap<>();
        data.put("attendanceId", ex.getAttendanceId());
        data.put("registeredAt", ex.getRegisteredAt());

        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                false,
                ex.getMessage(),
                data,
                "ALREADY_REGISTERED",
                java.time.LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(SessionInactiveException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleSessionInactive(SessionInactiveException ex) {
        Map<String, Object> data = new HashMap<>();
        data.put("endTime", ex.getEndTime());

        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                false,
                ex.getMessage(),
                data,
                "SESSION_INACTIVE",
                java.time.LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), "EMAIL_ALREADY_EXISTS"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                false,
                "Error de validación",
                errors,
                "VALIDATION_ERROR",
                java.time.LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error", "INTERNAL_SERVER_ERROR"));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), "CONFLICT"));
    }

    @ExceptionHandler(UserDisabledException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserDisabled(UserDisabledException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

}