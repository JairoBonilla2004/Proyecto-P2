package ec.edu.espe.SecureFrameGallery.modules.gallery.controllers;

import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.auth.repositories.UserRepository;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.GalleryService;
import ec.edu.espe.SecureFrameGallery.modules.steganography.dtos.QuarantineLogResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.steganography.services.QuarantineService;
import ec.edu.espe.SecureFrameGallery.shared.dtos.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/supervisor")
@RequiredArgsConstructor
@Tag(name = "Supervisor", description = "Gestión de cuarentena y aprobaciones — solo SUPERVISOR")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SUPERVISOR')")
public class SupervisorController {
    private final GalleryService galleryService;
    private final QuarantineService quarantineService;
    private final UserRepository userRepository;

    //  Gestión de Álbumes
    @GetMapping("/albums/pending")
    @Operation(summary = "Listar álbumes pendientes de revisión")
    public ResponseEntity<ApiResponse<List<AlbumResponseDto>>> getPendingAlbums() {
        return ResponseEntity.ok(ApiResponse.ok(galleryService.getPendingAlbums()));
    }

    @PutMapping("/albums/{albumId}/approve")
    @Operation(summary = "Aprobar un álbum")
    public ResponseEntity<ApiResponse<String>> approveAlbum(
            @PathVariable UUID albumId,
            Authentication authentication) {
        User supervisor = resolveUser(authentication);
        galleryService.approveAlbum(albumId, supervisor);
        return ResponseEntity.ok(ApiResponse.ok("Álbum aprobado", albumId.toString()));
    }

    @PutMapping("/albums/{albumId}/reject")
    @Operation(summary = "Rechazar un álbum")
    public ResponseEntity<ApiResponse<String>> rejectAlbum(
            @PathVariable UUID albumId,
            Authentication authentication) {
        User supervisor = resolveUser(authentication);
        galleryService.rejectAlbum(albumId, supervisor);
        return ResponseEntity.ok(ApiResponse.ok("Álbum rechazado", albumId.toString()));
    }

    @GetMapping("/quarantine")
    @Operation(summary = "Listar imágenes en cuarentena pendientes de revisión")
    public ResponseEntity<ApiResponse<List<QuarantineLogResponseDto>>> getQuarantineQueue() {
        List<QuarantineLogResponseDto> queue = quarantineService.getPendingQuarantine();
        return ResponseEntity.ok(ApiResponse.ok(queue));
    }

    @GetMapping("/quarantine/history")
    @Operation(summary = "Historial completo de revisiones de cuarentena")
    public ResponseEntity<ApiResponse<List<QuarantineLogResponseDto>>> getQuarantineHistory() {
        List<QuarantineLogResponseDto> history = quarantineService.getReviewHistory();
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @PutMapping("/images/{imageId}/approve")
    @Operation(summary = "Aprobar imagen en cuarentena")
    public ResponseEntity<ApiResponse<String>> approveImage(
            @PathVariable UUID imageId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        User supervisor = resolveUser(authentication);
        String notes = body != null ? body.getOrDefault("notes", "") : "";
        galleryService.approveImage(imageId, supervisor, notes);
        return ResponseEntity.ok(ApiResponse.ok("Imagen aprobada", imageId.toString()));
    }

    @PutMapping("/images/{imageId}/reject")
    @Operation(summary = "Rechazar imagen en cuarentena")
    public ResponseEntity<ApiResponse<String>> rejectImage(
            @PathVariable UUID imageId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        User supervisor = resolveUser(authentication);
        String notes = body != null ? body.getOrDefault("notes", "") : "";
        galleryService.rejectImage(imageId, supervisor, notes);
        return ResponseEntity.ok(ApiResponse.ok("Imagen rechazada", imageId.toString()));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Supervisor no encontrado"));
    }
}
