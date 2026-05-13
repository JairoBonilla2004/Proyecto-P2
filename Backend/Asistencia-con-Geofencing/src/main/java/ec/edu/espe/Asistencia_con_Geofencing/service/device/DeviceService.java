package ec.edu.espe.Asistencia_con_Geofencing.service.device;


import ec.edu.espe.Asistencia_con_Geofencing.dto.request.DeviceRegisterRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.UpdateFcmTokenRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.DeviceResponse;

import java.util.UUID;

public interface DeviceService {

    DeviceResponse registerDevice(UUID userId, DeviceRegisterRequest request);

    void updateFcmToken(UUID userId, UUID deviceId, UpdateFcmTokenRequest request);
    
    void deactivateDevice(UUID userId, UUID deviceId);
    

    void deactivateDeviceByIdentifier(UUID userId, String deviceIdentifier);
}
