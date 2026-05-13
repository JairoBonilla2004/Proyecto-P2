package ec.edu.espe.SecureFrameGallery.modules.gallery.services;

import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumCreateDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Album;
import ec.edu.espe.SecureFrameGallery.modules.gallery.repositories.AlbumRepository;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.impl.GalleryServiceImpl;
import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.auth.repositories.UserRepository;
import ec.edu.espe.SecureFrameGallery.shared.enums.AlbumStatus;
import ec.edu.espe.SecureFrameGallery.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GalleryServiceImpl — gestión de álbumes")
class GalleryServiceImplTest {

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GalleryServiceImpl galleryService;

    private User owner;
    private Album pendingAlbum;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("foto@espe.edu.ec");
        owner.setRole(Role.USER);
        owner.setEnabled(true);

        pendingAlbum = new Album();
        pendingAlbum.setId(UUID.randomUUID());
        pendingAlbum.setTitle("Fotos del campus");
        pendingAlbum.setApprovalStatus(AlbumStatus.PENDING_REVIEW);
        pendingAlbum.setOwner(owner);
        pendingAlbum.setPublic(true);
    }

    @Test
    @DisplayName("Crear álbum queda en estado PENDING_REVIEW")
    void createAlbumStartsAsPending() {
        AlbumCreateDto dto = new AlbumCreateDto();
        dto.setTitle("Nuevo álbum");
        dto.setPublic(true);

        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> {
            Album a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AlbumResponseDto result = galleryService.createAlbum(dto, owner.getEmail());

        assertNotNull(result);
        assertEquals(AlbumStatus.PENDING_REVIEW, result.getApprovalStatus());
        verify(albumRepository).save(any(Album.class));
    }

    @Test
    @DisplayName("Crear álbum falla si el usuario no existe")
    void createAlbumFailsIfUserNotFound() {
        AlbumCreateDto dto = new AlbumCreateDto();
        dto.setTitle("Álbum huérfano");

        when(userRepository.findByEmail("noexiste@espe.edu.ec")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
            () -> galleryService.createAlbum(dto, "noexiste@espe.edu.ec"));
        verify(albumRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar álbumes públicos solo retorna APPROVED")
    void listPublicAlbumsReturnsOnlyApproved() {
        Album approved = new Album();
        approved.setId(UUID.randomUUID());
        approved.setTitle("Aprobado");
        approved.setApprovalStatus(AlbumStatus.APPROVED);
        approved.setOwner(owner);
        approved.setPublic(true);

        when(albumRepository.findByApprovalStatusAndIsPublicTrue(AlbumStatus.APPROVED))
            .thenReturn(List.of(approved));

        List<AlbumResponseDto> results = galleryService.getPublicAlbums();

        assertEquals(1, results.size());
        assertEquals(AlbumStatus.APPROVED, results.get(0).getApprovalStatus());
    }

    @Test
    @DisplayName("Listar álbumes públicos retorna lista vacía si no hay aprobados")
    void listPublicAlbumsReturnsEmptyWhenNoneApproved() {
        when(albumRepository.findByApprovalStatusAndIsPublicTrue(AlbumStatus.APPROVED))
            .thenReturn(List.of());
        assertTrue(galleryService.getPublicAlbums().isEmpty());
    }

    @Test
    @DisplayName("Supervisor aprueba álbum pendiente correctamente")
    void supervisorApprovesAlbum() {
        when(albumRepository.findById(pendingAlbum.getId())).thenReturn(Optional.of(pendingAlbum));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals(AlbumStatus.APPROVED, galleryService.approveAlbum(pendingAlbum.getId()).getApprovalStatus());
    }

    @Test
    @DisplayName("Supervisor rechaza álbum pendiente correctamente")
    void supervisorRejectsAlbum() {
        when(albumRepository.findById(pendingAlbum.getId())).thenReturn(Optional.of(pendingAlbum));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals(AlbumStatus.REJECTED, galleryService.rejectAlbum(pendingAlbum.getId()).getApprovalStatus());
    }

    @Test
    @DisplayName("Aprobar álbum inexistente lanza excepción")
    void approveNonexistentAlbumThrows() {
        UUID fakeId = UUID.randomUUID();
        when(albumRepository.findById(fakeId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> galleryService.approveAlbum(fakeId));
    }

    @Test
    @DisplayName("Álbum rechazado no aparece en listado público")
    void rejectedAlbumDoesNotAppearInPublicList() {
        when(albumRepository.findByApprovalStatusAndIsPublicTrue(AlbumStatus.APPROVED))
            .thenReturn(List.of());

        List<AlbumResponseDto> results = galleryService.getPublicAlbums();

        assertTrue(results.stream().noneMatch(a -> a.getApprovalStatus() == AlbumStatus.REJECTED));
    }
}
