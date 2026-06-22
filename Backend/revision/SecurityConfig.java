package com.vulnerable.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    // ============================================================
    // A02:2025 - Security Misconfiguration
    // A01:2025 - Broken Access Control (CORS permisivo)
    // ============================================================

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")           // Permite cualquier origen
                .allowedMethods("*")           // Permite cualquier método
                .allowedHeaders("*")           // Permite cualquier header
                .allowCredentials(false);      // Inseguro
    }

    // Nota: No hay configuración de Spring Security
    // - CSRF deshabilitado por defecto
    // - Sin autenticación requerida
    // - Sin protección contra clickjacking
    // - Sin cabeceras de seguridad (HSTS, XSS Protection, etc.)
}