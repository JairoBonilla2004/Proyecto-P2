package ec.edu.espe.Asistencia_con_Geofencing.controller;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.DeviceRegisterRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.UpdateFcmTokenRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ApiResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.DeviceResponse;
import ec.edu.espe.Asistencia_con_Geofencing.security.CustomUserDetails;
import ec.edu.espe.Asistencia_con_Geofencing.service.device.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DeviceResponse>> registerDevice(
            @Valid @RequestBody DeviceRegisterRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        DeviceResponse response =
                deviceService.registerDevice(userDetails.getId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Dispositivo registrado exitosamente", response)
        );
    }

    @PutMapping("/{deviceId}/fcm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> updateFcmToken(
            @PathVariable UUID deviceId,
            @Valid @RequestBody UpdateFcmTokenRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        deviceService.updateFcmToken(userDetails.getId(), deviceId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Token FCM actualizado", null)
        );
    }
    
    @PostMapping("/{deviceId}/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> deactivateDevice(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        deviceService.deactivateDevice(userDetails.getId(), deviceId);

        return ResponseEntity.ok(
                ApiResponse.success("Dispositivo desactivado", null)
        );
    }
    
    @PostMapping("/logout-device")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> logoutDevice(
            @RequestParam String deviceIdentifier,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        deviceService.deactivateDeviceByIdentifier(userDetails.getId(), deviceIdentifier);

        return ResponseEntity.ok(
                ApiResponse.success("Dispositivo desactivado (logout exitoso)", null)
        );
    }
}
