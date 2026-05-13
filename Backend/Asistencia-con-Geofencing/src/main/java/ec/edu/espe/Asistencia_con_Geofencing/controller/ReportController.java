package ec.edu.espe.Asistencia_con_Geofencing.controller;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.GenerateReportRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ApiResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.ReportResponse;
import ec.edu.espe.Asistencia_con_Geofencing.security.CustomUserDetails;
import ec.edu.espe.Asistencia_con_Geofencing.service.report.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	@PostMapping("/generate")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<ReportResponse>> generateReport(

			@Valid @RequestBody GenerateReportRequest request,
			@AuthenticationPrincipal CustomUserDetails userDetails) {

		ReportResponse response =
				reportService.generateReport(request, userDetails.getId());

		return ResponseEntity.ok(
				ApiResponse.success("Reporte generándose", response)
		);
	}

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Page<ReportResponse>>> getMyReports(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			Pageable pageable) {

		Page<ReportResponse> reports =
				reportService.getMyReports(userDetails.getId(), pageable);

		return ResponseEntity.ok(ApiResponse.success(reports));
	}

	@GetMapping("/{id}/download")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Resource> downloadReport(
			@PathVariable UUID id,
			@AuthenticationPrincipal CustomUserDetails userDetails) {

		Resource resource =
				reportService.downloadReport(id, userDetails.getId());

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(
						HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"reporte-" + id + ".pdf\""
				)
				.body(resource);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Void>> deleteReport(
			@PathVariable UUID id,
			@AuthenticationPrincipal CustomUserDetails userDetails) {

		reportService.deleteReport(id, userDetails.getId());

		return ResponseEntity.ok(
				ApiResponse.success("Reporte eliminado exitosamente")
		);
	}
}
