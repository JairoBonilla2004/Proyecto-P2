package ec.edu.espe.Asistencia_con_Geofencing.controller;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.RegisterSensorEventRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ApiResponse;
import ec.edu.espe.Asistencia_con_Geofencing.security.CustomUserDetails;
import ec.edu.espe.Asistencia_con_Geofencing.service.sensor.SensorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorEventService;

    @PostMapping("/events")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerSensorEvent(
            @Valid @RequestBody RegisterSensorEventRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID eventId =
                sensorEventService.registerSensorEvent(userDetails.getId(), request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Sensor registrado exitosamente",
                        Map.of("eventId", eventId)
                )
        );
    }
}
