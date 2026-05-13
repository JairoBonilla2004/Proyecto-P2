package ec.edu.espe.SecureFrameGallery.application.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.espe.SecureFrameGallery.shared.dtos.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        log.warn("401 Unauthorized - URI: {} - Razón: {}", request.getRequestURI(), authException.getMessage());
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        String message = determineErrorMessage(authException);
        String details = "Proporciona un token JWT válido en el header Authorization. Formato: Bearer <token>";
        
        ApiResponse<Void> errorResponse = ApiResponse.error(
            message,
            details,
            HttpServletResponse.SC_UNAUTHORIZED
        );
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }

    private String determineErrorMessage(AuthenticationException ex) {
        if (ex.getMessage() == null) {
            return "No estás autenticado";
        }
        
        String message = ex.getMessage().toLowerCase();
        
        if (message.contains("expired")) {
            return "Tu token JWT ha expirado. Por favor, inicia sesión nuevamente.";
        }
        
        if (message.contains("invalid") || message.contains("malformed") || 
            message.contains("corrupted") || message.contains("not parseable")) {
            return "Token JWT inválido o corrupto. Por favor, verifica tu token.";
        }
        
        if (message.contains("bearer") || message.contains("token")) {
            return "Token JWT faltante o inválido en el header Authorization.";
        }
        
        return "No estás autenticado. Por favor, proporciona un token JWT válido.";
    }
}
