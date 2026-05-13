package ec.edu.espe.Asistencia_con_Geofencing.controller;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.CreateGeofenceZoneRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.ValidateLocationRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ApiResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.GeofenceZoneResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ValidateLocationResponse;
import ec.edu.espe.Asistencia_con_Geofencing.service.geofence.GeofenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/geofence")
@RequiredArgsConstructor
public class GeofenceController {

    private final GeofenceService geofenceService;

    @GetMapping("/zones")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<GeofenceZoneResponse>>> getAllZones() {
        List<GeofenceZoneResponse> zones = geofenceService.getAllZones();
        return ResponseEntity.ok(ApiResponse.success(zones));
    }

    @PostMapping("/zones")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GeofenceZoneResponse>> createZone(@Valid @RequestBody CreateGeofenceZoneRequest request) {
        GeofenceZoneResponse zone = geofenceService.createZone(request);
        return ResponseEntity.ok(ApiResponse.success("Zona creada exitosamente", zone));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ValidateLocationResponse>> validateLocation(@Valid @RequestBody ValidateLocationRequest request) {
        ValidateLocationResponse response = geofenceService.validateLocation(request);
        String message = response.getWithinCampus() ? "Ubicación válida" : "Ubicación fuera de rango";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @DeleteMapping("/zones/{zoneId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteZone(@PathVariable UUID zoneId) {
        geofenceService.deleteZone(zoneId);
        return ResponseEntity.ok(ApiResponse.success("Zona eliminada exitosamente", null));
    }

}