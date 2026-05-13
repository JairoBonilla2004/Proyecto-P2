package ec.edu.espe.Asistencia_con_Geofencing.config;

import ec.edu.espe.Asistencia_con_Geofencing.model.Role;
import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.ProviderType;
import ec.edu.espe.Asistencia_con_Geofencing.repository.RoleRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeRoles();
        initializeDefaultUsers();
    }

    private void initializeRoles() {
        List<String> roleNames = Arrays.asList("STUDENT", "TEACHER");

        for (String roleName : roleNames) {
            if (!roleRepository.findByName(roleName).isPresent()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                log.info("Rol creado: {}", roleName);
            }
        }
    }

    private void initializeDefaultUsers() {
        // Crear usuario TEACHER por defecto
        if (!userRepository.findByEmail("docente@espe.edu.ec").isPresent()) {
            Role teacherRole = roleRepository.findByName("TEACHER")
                    .orElseThrow(() -> new RuntimeException("Rol TEACHER no encontrado"));

            User teacher = new User();
            teacher.setEmail("docente@espe.edu.ec");
            teacher.setPassword(passwordEncoder.encode("docente123"));
            teacher.setFullName("Docente Demo");
            teacher.setProvider(ProviderType.LOCAL);
            teacher.setEnabled(true);
            teacher.setCreatedAt(LocalDateTime.now());
            
            Set<Role> roles = new HashSet<>();
            roles.add(teacherRole);
            teacher.setRoles(roles);
            
            userRepository.save(teacher);
            log.info("Usuario TEACHER creado: docente@espe.edu.ec / docente123");
        }
    }
}