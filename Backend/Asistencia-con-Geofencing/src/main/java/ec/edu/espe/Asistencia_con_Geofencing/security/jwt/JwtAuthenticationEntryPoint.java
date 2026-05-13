package ec.edu.espe.Asistencia_con_Geofencing.security.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence( //se ejecuta cuando un uario no autenticado intenta acceder a un recurso protegido
                        HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    Map<String, Object> errorResponse = Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", HttpServletResponse.SC_UNAUTHORIZED,
            "error", HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            "message", authException.getMessage(),
            "path", request.getServletPath()
    );
    String jsonResponse = objectMapper.writeValueAsString(errorResponse);
    response.getWriter().write(jsonResponse); //escribir la respuesta json en el body de la respuesta

  }
}
