package ec.edu.espe.SecureFrameGallery.modules.gallery.controllers;

import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.auth.repositories.UserRepository;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumCreateDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.GalleryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlbumController — endpoints de álbumes")
class AlbumControllerTest {

    @Mock
    private GalleryService galleryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AlbumController albumController;

    // ── GET /albums (público) ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /albums retorna 200 con lista de álbumes aprobados")
    void getPublicAlbumsReturns200() {
        AlbumResponseDto dto = AlbumResponseDto.builder()
                .id(UUID.randomUUID())
                .title("Álbum público")
                .build();

        when(galleryService.getPublicApprovedAlbums()).thenReturn(List.of(dto));

        ResponseEntity<?> response = albumController.getPublicAlbums();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("GET /albums retorna lista vacía si no hay álbumes aprobados")
    void getPublicAlbumsReturnsEmptyList() {
        when(galleryService.getPublicApprovedAlbums()).thenReturn(List.of());

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

        User owner = User.builder()
                .id(UUID.randomUUID())
                .email("user@espe.edu.ec")
                .build();

        AlbumResponseDto created = AlbumResponseDto.builder()
                .id(UUID.randomUUID())
                .title("Mi álbum")
                .build();

        when(authentication.getName()).thenReturn("user@espe.edu.ec");
        when(userRepository.findByEmail("user@espe.edu.ec")).thenReturn(java.util.Optional.of(owner));
        when(galleryService.createAlbum(any(AlbumCreateDto.class), any(User.class))).thenReturn(created);

        ResponseEntity<?> response = albumController.createAlbum(request, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /albums álbum creado inicia en PENDING_REVIEW")
    void createdAlbumStartsPending() {
        AlbumCreateDto request = new AlbumCreateDto();
        request.setTitle("Nuevo álbum");

        User owner = User.builder()
            .id(UUID.randomUUID())
            .email("user@espe.edu.ec")
            .build();

        AlbumResponseDto created = AlbumResponseDto.builder()
            .id(UUID.randomUUID())
            .title("Nuevo álbum")
            .build();

        when(authentication.getName()).thenReturn("user@espe.edu.ec");
        when(userRepository.findByEmail("user@espe.edu.ec")).thenReturn(java.util.Optional.of(owner));
        when(galleryService.createAlbum(any(), any(User.class))).thenReturn(created);

        ResponseEntity<?> response = albumController.createAlbum(request, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // ── GET /albums/mine ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /albums/mine retorna solo álbumes del usuario autenticado")
    void getMyAlbumsReturnsOwnAlbums() {
        AlbumResponseDto mine = AlbumResponseDto.builder()
                .id(UUID.randomUUID())
                .title("Mi álbum personal")
                .build();

        User owner = User.builder()
                .id(UUID.randomUUID())
                .email("user@espe.edu.ec")
                .build();

        when(authentication.getName()).thenReturn("user@espe.edu.ec");
        when(userRepository.findByEmail("user@espe.edu.ec")).thenReturn(java.util.Optional.of(owner));
        when(galleryService.getMyAlbums(owner)).thenReturn(List.of(mine));

        ResponseEntity<?> response = albumController.getMyAlbums(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
