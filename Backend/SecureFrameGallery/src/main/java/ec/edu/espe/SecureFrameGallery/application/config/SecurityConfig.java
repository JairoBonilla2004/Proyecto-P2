package ec.edu.espe.SecureFrameGallery.application.config;

import ec.edu.espe.SecureFrameGallery.application.security.JwtAccessDeniedHandler;
import ec.edu.espe.SecureFrameGallery.application.security.JwtAuthenticationEntryPoint;
import ec.edu.espe.SecureFrameGallery.application.security.JwtAuthenticationFilter;
import ec.edu.espe.SecureFrameGallery.application.security.OAuth2SuccessHandler;
import ec.edu.espe.SecureFrameGallery.application.security.RateLimitingFilter;
import ec.edu.espe.SecureFrameGallery.application.security.SecurityHeadersFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)  // 401 Unauthorized
                .accessDeniedHandler(jwtAccessDeniedHandler)           // 403 Forbidden
            )
                        .authorizeHttpRequests(auth -> auth
                // Públicas
                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                    "/actuator/info", "/actuator/health/**",
                    "/favicon.ico"
                ).permitAll()
                
                .requestMatchers("/login/**").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                // Albums - GET público, POST autenticado
                .requestMatchers(HttpMethod.GET, "/api/v1/albums").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/albums/*/images").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/albums").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/albums/mine").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/albums/*/images").authenticated()
                .requestMatchers("/api/v1/supervisor/**").hasRole("SUPERVISOR")
                .anyRequest().authenticated()
            )
            
            .oauth2Login(oauth2 -> oauth2 //solo para login con Google, no afecta a JWT
                .successHandler(oauth2SuccessHandler)
            )
                .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                .frameOptions(fo -> fo.deny())
                .xssProtection(xss -> xss.disable())
            );

        http.addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
            "https://secureframe-gallery.com",
            "http://localhost:5173"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
