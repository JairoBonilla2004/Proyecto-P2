package ec.edu.espe.SecureFrameGallery.modules.auth.services;

import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.LoginRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.RegisterRequest;
import ec.edu.espe.SecureFrameGallery.modules.auth.dtos.TokenResponse;
import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;


public interface AuthService {


    User register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse processOAuth2Login(String email, String name);
}
