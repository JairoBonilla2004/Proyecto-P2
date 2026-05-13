package ec.edu.espe.SecureFrameGallery.modules.steganography.services.impl;

import ec.edu.espe.SecureFrameGallery.modules.steganography.dtos.QuarantineLogResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.steganography.entities.QuarantineLog;
import ec.edu.espe.SecureFrameGallery.modules.steganography.mappers.QuarantineMapper;
import ec.edu.espe.SecureFrameGallery.modules.steganography.repositories.QuarantineLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Spy;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuarantineServiceImpl — revisión manual de imágenes sospechosas")
class QuarantineServiceImplTest {

    @Mock
    private QuarantineLogRepository quarantineLogRepository;

    @Spy
    private QuarantineMapper mapper = new QuarantineMapper();

    @InjectMocks
    private QuarantineServiceImpl quarantineService;

    private QuarantineLog quarantineLog;

    @BeforeEach
    void setUp() {
        ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Image suspiciousImage =
            ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Image.builder()
                .id(UUID.randomUUID())
                .originalName("sospechosa.jpg")
                .storedUrl("https://example.com/img.jpg")
                .imageStatus(ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus.QUARANTINED)
                .build();

        quarantineLog = new QuarantineLog();
        quarantineLog.setId(UUID.randomUUID());
        quarantineLog.setImage(suspiciousImage);
        quarantineLog.setDetectionReason("LSB entropy: 0.997");
        quarantineLog.setLsbScore(0.997);
        quarantineLog.setEofAnomaly(false);
    }

    @Test
    @DisplayName("Listar cuarentena retorna solo imágenes no revisadas")
    void listQuarantineReturnsPendingReview() {
        when(quarantineLogRepository.findPendingQuarantine()).thenReturn(List.of(quarantineLog));

        List<QuarantineLogResponseDto> result = quarantineService.getPendingQuarantine();

        assertEquals(1, result.size());
        assertEquals(quarantineLog.getImage().getId(), result.get(0).getImageId());
    }

    @Test
    @DisplayName("Listar cuarentena vacía retorna lista vacía")
    void listQuarantineReturnsEmptyWhenClean() {
        when(quarantineLogRepository.findPendingQuarantine()).thenReturn(List.of());
        assertTrue(quarantineService.getPendingQuarantine().isEmpty());
    }
}
