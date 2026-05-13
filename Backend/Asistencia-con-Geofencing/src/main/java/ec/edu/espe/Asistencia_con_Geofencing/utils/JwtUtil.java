package ec.edu.espe.Asistencia_con_Geofencing.utils;

import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}")
  private Long expiration;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes());
  }

  public String generateToken(UUID userId, String email, List<String> roles) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId.toString());
    claims.put("email", email);
    claims.put("roles", roles);

    return Jwts.builder()
            .claims(claims)
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
  }

  @Deprecated
  public String generateToken(UUID userId, String email) {
    return generateToken(userId, email, List.of());
  }

  public Claims extractClaims(String token) {
    return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
  }

  public String extractEmail(String token) {
    return extractClaims(token).getSubject();
  }

  public UUID extractUserId(String token) {
    String userIdStr = extractClaims(token).get("userId", String.class);
    return UUID.fromString(userIdStr);
  }

  public boolean isTokenExpired(String token) {
    return extractClaims(token).getExpiration().before(new Date());
  }

  public boolean validateToken(String token) {
    return !isTokenExpired(token);
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String email = extractEmail(token);
    return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }
}
