package ec.edu.espe.Asistencia_con_Geofencing.service.device;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.DeviceRegisterRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.UpdateFcmTokenRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.DeviceResponse;
import ec.edu.espe.Asistencia_con_Geofencing.model.Device;
import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.PlatformType;
import ec.edu.espe.Asistencia_con_Geofencing.repository.DeviceRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    @Override
    public DeviceResponse registerDevice(UUID userId, DeviceRegisterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Ultimo dispositivo con el mismo deviceIdentifier registrado por OTRO usuario
        List<Device> otherUserDevices = deviceRepository
                .findByDeviceIdentifierAndUserIdNot(request.getDeviceIdentifier(), userId);
        otherUserDevices.forEach(this::deactivateDeviceInternal); // equivalente a forEach( device -> deactivateDeviceInternal(device) )
        Device device = deviceRepository
                .findByUserIdAndDeviceIdentifier(userId, request.getDeviceIdentifier())
                .orElseGet(Device::new); // nuevo dispositivo si no existe

        PlatformType platform = configureAndActivateDevice(device, user, request);
        
        log.info("Dispositivo {} registrado para usuario {} (plataforma: {})", 
            request.getDeviceIdentifier(), userId, platform);

        return DeviceResponse.builder()
                .deviceId(device.getId())
                .registered(true)
                .build();
    }

    @Override
    public void updateFcmToken(UUID userId, UUID deviceId, UpdateFcmTokenRequest request) {
        Device device = findDeviceAndValidateOwnership(deviceId, userId);
        device.setFcmToken(request.getFcmToken());
        deviceRepository.save(device);
    }

    @Override
    public void deactivateDevice(UUID userId, UUID deviceId) {
        Device device = findDeviceAndValidateOwnership(deviceId, userId);
        deactivateDeviceInternal(device);
    }
    
    @Override
    public void deactivateDeviceByIdentifier(UUID userId, String deviceIdentifier) {
        // Si deviceIdentifier es null o vacío, desactivar TODOS los dispositivos del usuario
        if (deviceIdentifier == null || deviceIdentifier.isEmpty()) {
            log.info("Desactivando TODOS los dispositivos del usuario {} (logout sin deviceIdentifier)", userId);
            List<Device> userDevices = deviceRepository.findByUserId(userId);
            userDevices.forEach(this::deactivateDeviceInternal);
            log.info("Se desactivaron {} dispositivos para el usuario {}", userDevices.size(), userId);
            return;
        }
        
        Device device = deviceRepository
                .findByUserIdAndDeviceIdentifier(userId, deviceIdentifier)
                .orElse(null);
        
        if (device == null) {
            log.warn("Dispositivo {} no encontrado para usuario {} (posiblemente ya desactivado)", 
                deviceIdentifier, userId);
            return;
        }
        
        log.info("Desactivando dispositivo {} para usuario {} (logout)", deviceIdentifier, userId);
        deactivateDeviceInternal(device);
        log.debug("Dispositivo desactivado exitosamente. Usuario ya no recibirá notificaciones en este dispositivo.");
    }


    private PlatformType configureAndActivateDevice(Device device, User user, DeviceRegisterRequest request) {
        device.setUser(user);
        device.setDeviceIdentifier(request.getDeviceIdentifier());
        
        PlatformType platform = detectPlatform(request.getPlatform(), request.getDeviceIdentifier());
        device.setPlatform(platform);
        
        device.setFcmToken(request.getFcmToken());
        device.setIsActive(true);
        deviceRepository.save(device);
        
        return platform;
    }

    private Device findDeviceAndValidateOwnership(UUID deviceId, UUID userId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado"));
        validateDeviceOwnership(device, userId);
        return device;
    }

    private void validateDeviceOwnership(Device device, UUID userId) {
        if (!device.getUser().getId().equals(userId)) {
            throw new RuntimeException("No tienes permiso para modificar este dispositivo");
        }
    }

    private void deactivateDeviceInternal(Device device) {
        device.setIsActive(false);
        deviceRepository.save(device);
    }

    private PlatformType detectPlatform(String platform, String deviceIdentifier) {
        if (platform != null && !platform.isEmpty()) {
            try {
                return PlatformType.valueOf(platform.toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.warn("Plataforma inválida proporcionada: {}. Detectando automáticamente.", platform);
            }
        }
        if (deviceIdentifier != null) {
            String identifier = deviceIdentifier.toLowerCase();
            if (identifier.startsWith("android_")) {
                log.debug("Plataforma detectada: ANDROID desde deviceIdentifier");
                return PlatformType.ANDROID;
            } else if (identifier.startsWith("ios_")) {
                log.debug("Plataforma detectada: IOS desde deviceIdentifier");
                return PlatformType.IOS;
            }
        }
        log.warn("No se pudo detectar plataforma. Usando ANDROID por defecto.");
        return PlatformType.ANDROID;
    }
}

