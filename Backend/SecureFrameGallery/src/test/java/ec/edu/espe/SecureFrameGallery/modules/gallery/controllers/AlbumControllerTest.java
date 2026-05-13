package ec.edu.espe.SecureFrameGallery.modules.gallery.controllers;

import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumCreateDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.GalleryService;
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
@DisplayName("AlbumController — endpoints de álbumes")
class AlbumControllerTest {

    @Mock
    private GalleryService galleryService;

    @Mock
    private Principal principal;

    @InjectMocks
    private AlbumController albumController;

    // ── GET /albums (público) ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /albums retorna 200 con lista de álbumes aprobados")
    void getPublicAlbumsReturns200() {
        AlbumResponseDto dto = new AlbumResponseDto();
        dto.setId(UUID.randomUUID());
        dto.setTitle("Álbum público");
        dto.setApprovalStatus(AlbumStatus.APPROVED);

        when(galleryService.getPublicAlbums()).thenReturn(List.of(dto));

        ResponseEntity<?> response = albumController.getPublicAlbums();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("GET /albums retorna lista vacía si no hay álbumes aprobados")
    void getPublicAlbumsReturnsEmptyList() {
        when(galleryService.getPublicAlbums()).thenReturn(List.of());

        ResponseEntity<?> response = albumController.getPublicAlbums();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── POST /albums ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /albums retorna 201 al crear álbum exitosamente")
    void createAlbumReturns201() {
        AlbumCreateDto request = new AlbumCreateDto();
        request.setTitle("Mi álbum");
        request.setDescription("Fotos del campus");
        request.setPublic(true);

        AlbumResponseDto created = new AlbumResponseDto();
        created.setId(UUID.randomUUID());
        created.setTitle("Mi álbum");
        created.setApprovalStatus(AlbumStatus.PENDING_REVIEW);

        when(principal.getName()).thenReturn("user@espe.edu.ec");
        when(galleryService.createAlbum(any(AlbumCreateDto.class), eq("user@espe.edu.ec")))
            .thenReturn(created);

        ResponseEntity<?> response = albumController.createAlbum(request, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /albums álbum creado inicia en PENDING_REVIEW")
    void createdAlbumStartsPending() {
        AlbumCreateDto request = new AlbumCreateDto();
        request.setTitle("Nuevo álbum");

        AlbumResponseDto created = new AlbumResponseDto();
        created.setApprovalStatus(AlbumStatus.PENDING_REVIEW);

        when(principal.getName()).thenReturn("user@espe.edu.ec");
        when(galleryService.createAlbum(any(), any())).thenReturn(created);

        ResponseEntity<?> response = albumController.createAlbum(request, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // ── GET /albums/mine ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /albums/mine retorna solo álbumes del usuario autenticado")
    void getMyAlbumsReturnsOwnAlbums() {
        AlbumResponseDto mine = new AlbumResponseDto();
        mine.setId(UUID.randomUUID());
        mine.setTitle("Mi álbum personal");

        when(principal.getName()).thenReturn("user@espe.edu.ec");
        when(galleryService.getAlbumsByOwner("user@espe.edu.ec")).thenReturn(List.of(mine));

        ResponseEntity<?> response = albumController.getMyAlbums(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── GET /albums/{id} ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /albums/{id} retorna 200 para álbum existente")
    void getAlbumByIdReturns200() {
        UUID id = UUID.randomUUID();
        AlbumResponseDto dto = new AlbumResponseDto();
        dto.setId(id);
        dto.setApprovalStatus(AlbumStatus.APPROVED);

        when(galleryService.getAlbumById(id)).thenReturn(dto);

        ResponseEntity<?> response = albumController.getAlbumById(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /albums/{id} lanza excepción para álbum inexistente")
    void getAlbumByIdThrowsForUnknownId() {
        UUID fakeId = UUID.randomUUID();
        when(galleryService.getAlbumById(fakeId)).thenThrow(new RuntimeException("No encontrado"));

        assertThrows(RuntimeException.class, () -> albumController.getAlbumById(fakeId));
    }
}
