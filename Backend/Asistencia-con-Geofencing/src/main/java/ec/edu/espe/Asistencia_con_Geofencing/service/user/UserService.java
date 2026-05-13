package ec.edu.espe.Asistencia_con_Geofencing.service.user;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.UpdateProfileRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.UpdateUserRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.UserProfileResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {
    UserProfileResponse getMyProfile(UUID userId);
    UserProfileResponse updateMyProfile(UpdateProfileRequest request, UUID userId);
}
