package ec.edu.espe.Asistencia_con_Geofencing.controller;


import ec.edu.espe.Asistencia_con_Geofencing.dto.request.SyncAttendancesRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ApiResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.AttendanceHistoryResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SyncResultResponse;
import ec.edu.espe.Asistencia_con_Geofencing.security.CustomUserDetails;
import ec.edu.espe.Asistencia_con_Geofencing.service.attendance.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/sync")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SyncResultResponse>> syncOfflineAttendances(
            @Valid @RequestBody SyncAttendancesRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SyncResultResponse response = attendanceService.syncOfflineAttendances(request, userDetails.getId());
        String message = response.getFailedCount() == 0
                ? "Sincronización completada"
                : "Sincronización parcial";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AttendanceHistoryResponse>> getMyHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        AttendanceHistoryResponse response = attendanceService.getMyHistory(userDetails.getId(), startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}