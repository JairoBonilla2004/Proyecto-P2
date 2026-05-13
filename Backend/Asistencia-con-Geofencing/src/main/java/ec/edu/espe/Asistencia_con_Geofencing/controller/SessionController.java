package ec.edu.espe.Asistencia_con_Geofencing.controller;

import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ApiResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.PagedSessionResponse;
import ec.edu.espe.Asistencia_con_Geofencing.security.CustomUserDetails;
import ec.edu.espe.Asistencia_con_Geofencing.service.attendance.AttendanceService;
import ec.edu.espe.Asistencia_con_Geofencing.service.session.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.CreateSessionRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.AttendanceResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SessionResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SessionWithDistanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final AttendanceService attendanceService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SessionResponse response = sessionService.createSession(request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Sesión creada exitosamente", response));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getActiveSessions() {
        List<SessionResponse> sessions = sessionService.getActiveSessions();
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/my-sessions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<PagedSessionResponse>> getMySessionsAsTeacher(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        Page<SessionResponse> sessionsPage = sessionService.getMySessionsAsTeacher(userDetails.getId(), pageable);
        
        PagedSessionResponse pagedResponse = PagedSessionResponse.builder()
                .sessions(sessionsPage.getContent())
                .totalPages(sessionsPage.getTotalPages())
                .totalElements(sessionsPage.getTotalElements())
                .currentPage(sessionsPage.getNumber())
                .pageSize(sessionsPage.getSize())
                .hasNext(sessionsPage.hasNext())
                .hasPrevious(sessionsPage.hasPrevious())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/{id}/attendances")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getSessionAttendances(@PathVariable UUID id) {
        List<AttendanceResponse> attendances = attendanceService.getSessionAttendances(id);
        return ResponseEntity.ok(ApiResponse.success(attendances));
    }

    @PostMapping("/{id}/end")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SessionResponse>> endSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SessionResponse response = sessionService.endSession(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Sesión finalizada correctamente", response));
    }

    @GetMapping("/active-with-distances")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SessionWithDistanceResponse>>> getActiveSessionsWithDistances(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
        List<SessionWithDistanceResponse> sessions = sessionService.getActiveSessionsWithDistances(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }
}