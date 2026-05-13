package ec.edu.espe.SecureFrameGallery.modules.gallery.controllers;

import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.auth.repositories.UserRepository;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.GalleryService;
import ec.edu.espe.SecureFrameGallery.modules.steganography.dtos.QuarantineLogResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.steganography.services.QuarantineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupervisorController — panel de revisión")
class SupervisorControllerTest {

    @Mock
    private GalleryService galleryService;

    @Mock
    private QuarantineService quarantineService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SupervisorController supervisorController;

    // ── Álbumes pendientes ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /supervisor/albums/pending retorna 200 con lista")
    void getPendingAlbumsReturns200() {
        AlbumResponseDto pending = AlbumResponseDto.builder()
            .id(UUID.randomUUID())
            .title("Álbum pendiente")
            .build();

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

        User supervisor = User.builder().id(UUID.randomUUID()).email("supervisor@espe.edu.ec").build();
        when(authentication.getName()).thenReturn("supervisor@espe.edu.ec");
        when(userRepository.findByEmail("supervisor@espe.edu.ec")).thenReturn(java.util.Optional.of(supervisor));
        doNothing().when(galleryService).approveAlbum(eq(id), eq(supervisor));

        ResponseEntity<?> response = supervisorController.approveAlbum(id, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("PUT /supervisor/albums/{id}/reject retorna 200")
    void rejectAlbumReturns200() {
        UUID id = UUID.randomUUID();

        User supervisor = User.builder().id(UUID.randomUUID()).email("supervisor@espe.edu.ec").build();
        when(authentication.getName()).thenReturn("supervisor@espe.edu.ec");
        when(userRepository.findByEmail("supervisor@espe.edu.ec")).thenReturn(java.util.Optional.of(supervisor));
        doNothing().when(galleryService).rejectAlbum(eq(id), eq(supervisor));

        ResponseEntity<?> response = supervisorController.rejectAlbum(id, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Aprobar álbum inexistente propaga excepción")
    void approveUnknownAlbumPropagatesException() {
        UUID fakeId = UUID.randomUUID();
        User supervisor = User.builder().id(UUID.randomUUID()).email("supervisor@espe.edu.ec").build();
        when(authentication.getName()).thenReturn("supervisor@espe.edu.ec");
        when(userRepository.findByEmail("supervisor@espe.edu.ec")).thenReturn(java.util.Optional.of(supervisor));
        doThrow(new RuntimeException("No encontrado")).when(galleryService).approveAlbum(eq(fakeId), eq(supervisor));

        assertThrows(RuntimeException.class, () -> supervisorController.approveAlbum(fakeId, authentication));
    }

    // ── Cuarentena ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /supervisor/quarantine retorna 200 con imágenes en cuarentena")
    void getQuarantineReturns200() {
        QuarantineLogResponseDto log = new QuarantineLogResponseDto();
        log.setId(UUID.randomUUID());
        log.setDetectionReason("LSB entropy: 0.998");

        when(quarantineService.getPendingQuarantine()).thenReturn(List.of(log));

        ResponseEntity<?> response = supervisorController.getQuarantineQueue();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("PUT /supervisor/images/{id}/approve aprueba imagen en cuarentena")
    void approveQuarantinedImageReturns200() {
        UUID imageId = UUID.randomUUID();
        User supervisor = User.builder().id(UUID.randomUUID()).email("supervisor@espe.edu.ec").build();
        when(authentication.getName()).thenReturn("supervisor@espe.edu.ec");
        when(userRepository.findByEmail("supervisor@espe.edu.ec")).thenReturn(java.util.Optional.of(supervisor));

        doNothing().when(galleryService).approveImage(eq(imageId), eq(supervisor), eq("Falso positivo"));

        ResponseEntity<?> response = supervisorController.approveImage(
                imageId,
                Map.of("notes", "Falso positivo"),
                authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("PUT /supervisor/images/{id}/reject rechaza imagen en cuarentena")
    void rejectQuarantinedImageReturns200() {
        UUID imageId = UUID.randomUUID();
        User supervisor = User.builder().id(UUID.randomUUID()).email("supervisor@espe.edu.ec").build();
        when(authentication.getName()).thenReturn("supervisor@espe.edu.ec");
        when(userRepository.findByEmail("supervisor@espe.edu.ec")).thenReturn(java.util.Optional.of(supervisor));

        doNothing().when(galleryService).rejectImage(eq(imageId), eq(supervisor), eq("Confirmado"));

        ResponseEntity<?> response = supervisorController.rejectImage(
                imageId,
                Map.of("notes", "Confirmado"),
                authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
