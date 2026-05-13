package ec.edu.espe.SecureFrameGallery.application.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.TokenResponse;
import ec.edu.espe.SecureFrameGallery.modules.auth.services.AuthService;
import ec.edu.espe.SecureFrameGallery.shared.dtos.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        log.info("OAuth2 login exitoso para: {}", email);
        TokenResponse tokenResponse = authService.processOAuth2Login(email, name);
        
        // Redirect to frontend with the token
        String redirectUrl = frontendUrl + "/oauth2/redirect?token=" + tokenResponse.getAccessToken();
        response.sendRedirect(redirectUrl);
    }
}
