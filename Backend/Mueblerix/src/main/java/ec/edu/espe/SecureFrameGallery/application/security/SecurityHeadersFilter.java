package ec.edu.espe.SecureFrameGallery.application.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de headers de seguridad.
 * RNF-Seguridad: Agrega headers HTTP que protegen contra ataques comunes
 * (XSS, Clickjacking, Type Confusion, HSTS).
 */
@Component
@Slf4j
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        // Content Security Policy: solo permite recursos del mismo origen
        response.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'");

        // Prevenir Type Confusion: obliga al navegador a respetar el Content-Type
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Prevenir Clickjacking: no permite que el sitio se cargue en un iframe
        response.setHeader("X-Frame-Options", "DENY");

        // Habilitar XSS Protection en navegadores antiguos
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // HSTS: fuerza HTTPS (en producción con max-age más largo)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Referrer Policy: no enviar referrer a otros orígenes
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Disable DNS Prefetching para evitar leaks de privacidad
        response.setHeader("X-DNS-Prefetch-Control", "off");

        filterChain.doFilter(request, response);
    }
}
