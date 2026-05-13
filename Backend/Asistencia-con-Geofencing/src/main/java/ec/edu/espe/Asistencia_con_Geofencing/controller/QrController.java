package ec.edu.espe.Asistencia_con_Geofencing.controller;


import ec.edu.espe.Asistencia_con_Geofencing.dto.request.GenerateQrRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.ValidateQrRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ApiResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.AttendanceResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.QrTokenResponse;
import ec.edu.espe.Asistencia_con_Geofencing.security.CustomUserDetails;
import ec.edu.espe.Asistencia_con_Geofencing.service.attendance.AttendanceService;
import ec.edu.espe.Asistencia_con_Geofencing.service.qr.QrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qr")
@RequiredArgsConstructor
public class QrController {

    private final QrService qrService;
    private final AttendanceService attendanceService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<QrTokenResponse>> generateQr(
            @Valid @RequestBody GenerateQrRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        QrTokenResponse response = qrService.generateQrToken(request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Código QR generado exitosamente", response));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> validateQr(
            @Valid @RequestBody ValidateQrRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AttendanceResponse response = attendanceService.validateQrAndRegisterAttendance(request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Asistencia registrada correctamente", response));
    }
}