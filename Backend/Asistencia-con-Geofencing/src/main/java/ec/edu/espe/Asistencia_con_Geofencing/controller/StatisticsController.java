package ec.edu.espe.Asistencia_con_Geofencing.controller;

import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ApiResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.DashboardResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SessionStatisticsResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.TeacherDashboardResponse;
import ec.edu.espe.Asistencia_con_Geofencing.security.CustomUserDetails;
import ec.edu.espe.Asistencia_con_Geofencing.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

	private final DashboardService dashboardService;

	@GetMapping("/dashboard")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<DashboardResponse>> getStudentDashboard(
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		DashboardResponse response = dashboardService.getStudentDashboard(userDetails.getId());
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping("/teacher/dashboard")
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<ApiResponse<TeacherDashboardResponse>> getTeacherDashboard(
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		TeacherDashboardResponse response = dashboardService.getTeacherDashboard(userDetails.getId());
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping("/session/{id}")
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<ApiResponse<SessionStatisticsResponse>> getSessionStatistics(
			@PathVariable UUID id,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		SessionStatisticsResponse response =
				dashboardService.getSessionStatistics(id, userDetails.getId());
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
