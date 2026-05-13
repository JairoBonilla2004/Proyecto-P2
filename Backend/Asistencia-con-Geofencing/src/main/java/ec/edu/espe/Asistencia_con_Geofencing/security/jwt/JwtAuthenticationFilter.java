package ec.edu.espe.Asistencia_con_Geofencing.security.jwt;

import ec.edu.espe.Asistencia_con_Geofencing.service.CustomUserDetailsService;
import ec.edu.espe.Asistencia_con_Geofencing.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final CustomUserDetailsService customUserDetailsService;

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private static final List<String> EXCLUDED_PATHS = List.of(// endpoints que no requieren autenticacion
          "/api/v1/auth/**",
          "/api/v1/public/**"
  );

  @Override
  protected void doFilterInternal(
          @NonNull HttpServletRequest request,
          @NonNull HttpServletResponse response,
          @NonNull FilterChain filterChain) throws ServletException, IOException {
    if (shouldSkipFilter(request)) {
      filterChain.doFilter(request, response);
      return;
    }
    try {
      String jwt = extractToken(request);
      if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        authenticateUser(jwt, request);
      }
    } catch (ExpiredJwtException e) {
      setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "JWT token has expired");
      return;

    } catch (UnsupportedJwtException e) {
      setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unsupported JWT token");
      return;

    } catch (MalformedJwtException e) {
      setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Malformed JWT token");
      return;

    } catch (SecurityException e) {
      setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT signature");
      return;

    } catch (IllegalArgumentException e) {
      setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
      return;

    } catch (Exception e) {
      setErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
      return;
    }
    filterChain.doFilter(request, response);
  }

  private boolean shouldSkipFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
      path = path.substring(contextPath.length());
    }

    final String finalPath = path;

    boolean shouldSkip = EXCLUDED_PATHS.stream().anyMatch(excludedPath -> {
      boolean matches = finalPath.equals(excludedPath) || finalPath.startsWith(excludedPath + "/");
      return matches;
    });
    return shouldSkip;
  }


  private String extractToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }
    return null;
  }

  private void authenticateUser(String token, HttpServletRequest request) {
    String email = jwtUtil.extractEmail(token);
    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
      if (jwtUtil.isTokenValid(token, userDetails)) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        log.debug("Usuario autenticado: {}", email);
      }
    }
  }

  private void setErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
    response.setStatus(statusCode);
    response.setContentType("application/json");
    String jsonResponse = String.format(
            "{\"error\": \"%s\", \"message\": \"%s\", \"timestamp\": \"%s\"}",
            getErrorType(statusCode),
            message,
            java.time.Instant.now().toString()
    );
    response.getWriter().write(jsonResponse);
  }

  private String getErrorType(int statusCode) {
    return switch (statusCode) {
      case HttpServletResponse.SC_UNAUTHORIZED -> "UNAUTHORIZED";
      case HttpServletResponse.SC_FORBIDDEN -> "FORBIDDEN";
      case HttpServletResponse.SC_INTERNAL_SERVER_ERROR -> "INTERNAL_SERVER_ERROR";
      default -> "ERROR";
    };
  }
}