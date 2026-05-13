package ec.edu.espe.SecureFrameGallery.modules.gallery.services;

import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumCreateDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Album;
import ec.edu.espe.SecureFrameGallery.modules.gallery.repositories.AlbumRepository;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.impl.GalleryServiceImpl;
import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.shared.enums.AlbumStatus;
import ec.edu.espe.SecureFrameGallery.modules.gallery.repositories.ImageRepository;
import ec.edu.espe.SecureFrameGallery.modules.steganography.repositories.QuarantineLogRepository;
import ec.edu.espe.SecureFrameGallery.modules.steganography.services.ImageAnalyzer;
import ec.edu.espe.SecureFrameGallery.modules.steganography.services.MetadataCleaner;
import ec.edu.espe.SecureFrameGallery.shared.utils.MagicNumberUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.util.List;
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
    private ImageRepository imageRepository;

    @Mock
    private QuarantineLogRepository quarantineLogRepository;

    @Mock
    private MagicNumberUtil magicNumberUtil;

    @Mock
    private ImageAnalyzer imageAnalyzer;

    @Mock
    private MetadataCleaner metadataCleaner;

    @Mock
    private CloudinaryStorageService cloudinaryStorageService;

    @InjectMocks
    private GalleryServiceImpl galleryService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("foto@espe.edu.ec");
        owner.setRole(ec.edu.espe.SecureFrameGallery.shared.enums.Role.ROLE_USER);
        owner.setEnabled(true);
    }

    @Test
    @DisplayName("Crear álbum queda en estado PENDING_REVIEW")
    void createAlbumStartsAsPending() {
        AlbumCreateDto dto = new AlbumCreateDto();
        dto.setTitle("Nuevo álbum");
        dto.setPublic(true);

        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> {
            Album a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(albumRepository.existsByOwnerAndTitleIgnoreCase(eq(owner), any())).thenReturn(false);

        AlbumResponseDto result = galleryService.createAlbum(dto, owner);

        assertNotNull(result);
        assertEquals(AlbumStatus.PENDING_REVIEW, result.getApprovalStatus());
        verify(albumRepository).save(any(Album.class));

        ArgumentCaptor<Album> captor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(captor.capture());
        assertEquals(AlbumStatus.PENDING_REVIEW, captor.getValue().getApprovalStatus());
        assertEquals(owner.getEmail(), captor.getValue().getOwner().getEmail());
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

        List<AlbumResponseDto> results = galleryService.getPublicApprovedAlbums();

        assertEquals(1, results.size());
        assertEquals(AlbumStatus.APPROVED, results.get(0).getApprovalStatus());
    }

    @Test
    @DisplayName("Listar álbumes públicos retorna lista vacía si no hay aprobados")
    void listPublicAlbumsReturnsEmptyWhenNoneApproved() {
        when(albumRepository.findByApprovalStatusAndIsPublicTrue(AlbumStatus.APPROVED))
            .thenReturn(List.of());
        assertTrue(galleryService.getPublicApprovedAlbums().isEmpty());
    }
}
