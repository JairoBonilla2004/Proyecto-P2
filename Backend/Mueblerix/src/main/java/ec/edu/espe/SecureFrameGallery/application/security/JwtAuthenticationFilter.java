package ec.edu.espe.SecureFrameGallery.application.security;

import ec.edu.espe.SecureFrameGallery.shared.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final String[] SKIP_PATHS = {
            "/oauth2/**",
            "/login/**",
            "/actuator/**"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // Omitir validación JWT para rutas de autenticación
        if (shouldSkipJwtValidation(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            try {
                if (jwtUtil.isValid(token)) {
                    String email = jwtUtil.extractEmail(token);
                    String role = jwtUtil.extractRole(token);
                    String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleWithPrefix);
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singleton(authority)
                        );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT validado para usuario: {} con rol: {}", email, role);
                } else {
                    log.warn(" JWT inválido o expirado");
                    SecurityContextHolder.clearContext();
                }
            } catch (Exception e) {
                log.warn("Error al validar JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
  
        filterChain.doFilter(request, response);
    }

  
    private boolean shouldSkipJwtValidation(String requestPath) {
        for (String skipPath : SKIP_PATHS) {
            if (pathMatchesPattern(requestPath, skipPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean pathMatchesPattern(String path, String pattern) {
        if (pattern.contains("/**")) {
            String prefix = pattern.replace("/**", "");
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
