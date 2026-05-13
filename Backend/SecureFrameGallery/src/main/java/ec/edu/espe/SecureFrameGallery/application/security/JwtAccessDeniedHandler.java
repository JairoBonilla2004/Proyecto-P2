package ec.edu.espe.SecureFrameGallery.application.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.espe.SecureFrameGallery.shared.dtos.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        log.warn(" 403 Forbidden - Acceso denegado - Usuario autenticado sin permisos suficientes - URI: {} - Usuario: {}", 
            request.getRequestURI(), 
            request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "unknown");
        
        try {
            // Preparar respuesta
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            
            ApiResponse<Void> errorResponse = ApiResponse.error(
                "No tienes permiso para acceder a este recurso",
                "Tu rol o nivel de acceso no te permite realizar esta acción. " +
                "Contacta al administrador si crees que es un error.",
                HttpServletResponse.SC_FORBIDDEN
            );
            
            // Serializar y escribir respuesta
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            log.debug("JSON de error 403 generado: {}", jsonResponse);
            
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
            response.getWriter().close();
            
        } catch (IOException e) {
            log.error("Error al escribir respuesta 403: {}", e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado");
            } catch (IOException ignored) {
                log.error("Imposible enviar error. Response ya fue comprometida.");
            }
        }
    }
}
