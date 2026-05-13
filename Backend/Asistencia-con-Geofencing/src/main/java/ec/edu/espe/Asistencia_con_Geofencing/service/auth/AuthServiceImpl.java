package ec.edu.espe.Asistencia_con_Geofencing.service.auth;

import ec.edu.espe.Asistencia_con_Geofencing.dto.mapper.UserMapper;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.*;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.AuthResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.UserResponse;
import ec.edu.espe.Asistencia_con_Geofencing.exception.EmailAlreadyExistsException;
import ec.edu.espe.Asistencia_con_Geofencing.exception.ResourceNotFoundException;
import ec.edu.espe.Asistencia_con_Geofencing.exception.UserDisabledException;
import ec.edu.espe.Asistencia_con_Geofencing.model.OAuthAccount;
import ec.edu.espe.Asistencia_con_Geofencing.model.Role;
import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.OAuthProvider;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.ProviderType;
import ec.edu.espe.Asistencia_con_Geofencing.repository.OAuthAccountRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.RoleRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.UserRepository;
import ec.edu.espe.Asistencia_con_Geofencing.service.device.DeviceService;
import ec.edu.espe.Asistencia_con_Geofencing.utils.JwtUtil;
import ec.edu.espe.Asistencia_con_Geofencing.utils.auth.factory.OAuthStrategyFactory;
import ec.edu.espe.Asistencia_con_Geofencing.utils.auth.strategy.OAuthLoginStrategy;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.OAuthUserData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final OAuthStrategyFactory oAuthStrategyFactory;
    private final DeviceService deviceService;

    @Transactional
    public AuthResponse register(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setProvider(ProviderType.LOCAL);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        String roleName = request.getRole().toUpperCase();
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleName));

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        request.validate();
        OAuthProvider provider = OAuthProvider.valueOf(request.getProvider().toUpperCase());
        OAuthLoginStrategy strategy = oAuthStrategyFactory.getStrategy(provider);
        OAuthUserData userData = strategy.validateAndExtractUserData(request.getToken());
        ProviderType providerType = strategy.getProviderType();
        User user = processOAuthLogin(
                userData.getProviderUserId(),
                userData.getEmail(),
                userData.getFullName(),
                provider,
                providerType
        );
        
        if (request.getFcmToken() != null && !request.getFcmToken().isEmpty() &&
            request.getDeviceIdentifier() != null && !request.getDeviceIdentifier().isEmpty()) {
            
            DeviceRegisterRequest deviceRequest = DeviceRegisterRequest.builder()
                    .deviceIdentifier(request.getDeviceIdentifier())
                    .fcmToken(request.getFcmToken())
                    .build();
            
            deviceService.registerDevice(user.getId(), deviceRequest);
        }
        
        return buildAuthResponse(user);
    }

    private User processOAuthLogin(String providerUserId, String email, String fullName,
                                    OAuthProvider oAuthProvider, ProviderType providerType) {
        OAuthAccount oAuthAccount = oAuthAccountRepository
                .findByProviderAndProviderUserId(oAuthProvider, providerUserId)
                .orElse(null);

        User user;
        if (oAuthAccount != null) {
            user = oAuthAccount.getUser();
        } else {
            user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setFullName(fullName);
                user.setProvider(providerType);
                user.setEnabled(true);
                user.setCreatedAt(LocalDateTime.now());

                Role studentRole = roleRepository.findByName("STUDENT")
                        .orElseThrow(() -> new RuntimeException("Rol STUDENT no encontrado"));
                Set<Role> roles = new HashSet<>();
                roles.add(studentRole);
                user.setRoles(roles);

                user = userRepository.save(user);
            }

            OAuthAccount newAccount = new OAuthAccount();
            newAccount.setUser(user);
            newAccount.setProvider(oAuthProvider);
            newAccount.setProviderUserId(providerUserId);
            newAccount.setCreatedAt(LocalDateTime.now());
            oAuthAccountRepository.save(newAccount);
        }

        if (!user.getEnabled()) {
            throw new UserDisabledException("Usuario deshabilitado");
        }

        return user;
    }

    private AuthResponse buildAuthResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), roles);
        UserResponse userResponse = userMapper.toResponse(user);
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .user(userResponse)
                .build();
    }
    
    @Override
    @Transactional
    public void logout(UUID userId, LogoutRequest request) {
        deviceService.deactivateDeviceByIdentifier(userId, request.getDeviceIdentifier());
    }
}
