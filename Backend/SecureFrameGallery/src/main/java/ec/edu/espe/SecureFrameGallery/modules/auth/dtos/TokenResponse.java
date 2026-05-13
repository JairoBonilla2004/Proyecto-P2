package ec.edu.espe.SecureFrameGallery.modules.auth.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String email;
    private String role;
}
