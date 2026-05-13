package ec.edu.espe.SecureFrameGallery.modules.gallery.controllers;

import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.GalleryService;
import ec.edu.espe.SecureFrameGallery.modules.steganography.dtos.QuarantineLogResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.steganography.services.QuarantineService;
import ec.edu.espe.SecureFrameGallery.shared.enums.AlbumStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupervisorController — panel de revisión")
class SupervisorControllerTest {

    @Mock
    private GalleryService galleryService;

    @Mock
    private QuarantineService quarantineService;

    @Mock
    private Principal principal;

    @InjectMocks
    private SupervisorController supervisorController;

    // ── Álbumes pendientes ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /supervisor/albums/pending retorna 200 con lista")
    void getPendingAlbumsReturns200() {
        AlbumResponseDto pending = new AlbumResponseDto();
        pending.setId(UUID.randomUUID());
        pending.setTitle("Álbum pendiente");
        pending.setApprovalStatus(AlbumStatus.PENDING_REVIEW);

        when(galleryService.getPendingAlbums()).thenReturn(List.of(pending));

        ResponseEntity<?> response = supervisorController.getPendingAlbums();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /supervisor/albums/pending retorna lista vacía si no hay pendientes")
    void getPendingAlbumsReturnsEmptyList() {
        when(galleryService.getPendingAlbums()).thenReturn(List.of());

        ResponseEntity<?> response = supervisorController.getPendingAlbums();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── Aprobar / rechazar álbum ──────────────────────────────────────────────

    @Test
    @DisplayName("PUT /supervisor/albums/{id}/approve retorna 200")
    void approveAlbumReturns200() {
        UUID id = UUID.randomUUID();
        AlbumResponseDto approved = new AlbumResponseDto();
        approved.setId(id);
        approved.setApprovalStatus(AlbumStatus.APPROVED);

        when(galleryService.approveAlbum(id)).thenReturn(approved);

        ResponseEntity<?> response = supervisorController.approveAlbum(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("PUT /supervisor/albums/{id}/reject retorna 200")
    void rejectAlbumReturns200() {
        UUID id = UUID.randomUUID();
        AlbumResponseDto rejected = new AlbumResponseDto();
        rejected.setId(id);
        rejected.setApprovalStatus(AlbumStatus.REJECTED);

        when(galleryService.rejectAlbum(id)).thenReturn(rejected);

        ResponseEntity<?> response = supervisorController.rejectAlbum(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Aprobar álbum inexistente propaga excepción")
    void approveUnknownAlbumPropagatesException() {
        UUID fakeId = UUID.randomUUID();
        when(galleryService.approveAlbum(fakeId)).thenThrow(new RuntimeException("No encontrado"));

        assertThrows(RuntimeException.class, () -> supervisorController.approveAlbum(fakeId));
    }

    // ── Cuarentena ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /supervisor/quarantine retorna 200 con imágenes en cuarentena")
    void getQuarantineReturns200() {
        QuarantineLogResponseDto log = new QuarantineLogResponseDto();
        log.setId(UUID.randomUUID());
        log.setDetectionReason("LSB entropy: 0.998");

        when(quarantineService.getPendingQuarantine()).thenReturn(List.of(log));

        ResponseEntity<?> response = supervisorController.getQuarantine();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("PUT /supervisor/images/{id}/approve aprueba imagen en cuarentena")
    void approveQuarantinedImageReturns200() {
        UUID logId = UUID.randomUUID();
        when(principal.getName()).thenReturn("supervisor@espe.edu.ec");

        doNothing().when(quarantineService)
            .approveImage(eq(logId), any(), any());

        ResponseEntity<?> response = supervisorController.approveQuarantinedImage(logId, "Falso positivo", principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("PUT /supervisor/images/{id}/reject rechaza imagen en cuarentena")
    void rejectQuarantinedImageReturns200() {
        UUID logId = UUID.randomUUID();
        when(principal.getName()).thenReturn("supervisor@espe.edu.ec");

        doNothing().when(quarantineService)
            .rejectImage(eq(logId), any(), any());

        ResponseEntity<?> response = supervisorController.rejectQuarantinedImage(logId, "Confirmado", principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
