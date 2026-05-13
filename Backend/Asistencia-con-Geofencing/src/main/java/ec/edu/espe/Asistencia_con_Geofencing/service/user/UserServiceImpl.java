package ec.edu.espe.Asistencia_con_Geofencing.service.user;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.UpdateProfileRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.UserProfileResponse;
import ec.edu.espe.Asistencia_con_Geofencing.exception.ResourceNotFoundException;
import ec.edu.espe.Asistencia_con_Geofencing.model.Role;
import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import ec.edu.espe.Asistencia_con_Geofencing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        List<UserProfileResponse.OAuthAccountInfo> oauthAccounts = user.getOauthAccounts().stream()
                .map(oauth -> UserProfileResponse.OAuthAccountInfo.builder()
                        .provider(oauth.getProvider().name())
                        .linkedAt(oauth.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .provider(user.getProvider().name())
                .roles(roles)
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .oauthAccounts(oauthAccounts)
                .build();
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setFullName(request.getFullName());
        user = userRepository.save(user);

        return getMyProfile(user.getId());
    }
}