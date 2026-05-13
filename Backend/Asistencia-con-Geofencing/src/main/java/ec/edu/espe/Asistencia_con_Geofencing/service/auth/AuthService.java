package ec.edu.espe.Asistencia_con_Geofencing.service.auth;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.AuthRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.LogoutRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.RegisterUserRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.AuthResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.UserResponse;

import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterUserRequest request);

    AuthResponse authenticate(AuthRequest request);

    void logout(UUID userId, LogoutRequest request);
}
