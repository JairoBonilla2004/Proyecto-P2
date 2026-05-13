package ec.edu.espe.Asistencia_con_Geofencing.dto.mapper;

import ec.edu.espe.Asistencia_con_Geofencing.dto.response.UserResponse;
import ec.edu.espe.Asistencia_con_Geofencing.model.Role;
import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .provider(user.getProvider())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .roles(user.getRoles() != null 
                    ? user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList())
                    : null)
                .build();
    }
}
