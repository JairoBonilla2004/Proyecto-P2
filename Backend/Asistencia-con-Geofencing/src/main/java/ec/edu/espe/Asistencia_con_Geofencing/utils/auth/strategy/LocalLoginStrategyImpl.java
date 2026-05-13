package ec.edu.espe.Asistencia_con_Geofencing.utils.auth.strategy;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.OAuthUserData;
import ec.edu.espe.Asistencia_con_Geofencing.exception.UnauthorizedException;
import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.OAuthProvider;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.ProviderType;
import ec.edu.espe.Asistencia_con_Geofencing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class LocalLoginStrategyImpl implements OAuthLoginStrategy {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public OAuthUserData validateAndExtractUserData(String credentials) {
        String decoded = new String(java.util.Base64.getDecoder().decode(credentials));
        String[] parts = decoded.split(":", 2);
        
        if (parts.length != 2) {
            throw new UnauthorizedException("Formato de credenciales inválido");
        }

        String email = parts[0];
        String password = parts[1];

        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        if (!user.getEnabled()) {
            throw new UnauthorizedException("Usuario deshabilitado");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }

        return OAuthUserData.builder()
                .providerUserId(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    @Override
    public OAuthProvider getOAuthProvider() {
        return OAuthProvider.LOCAL;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.LOCAL;
    }
}
