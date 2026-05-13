package ec.edu.espe.SecureFrameGallery.modules.steganography.services.impl;

import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Image;
import ec.edu.espe.SecureFrameGallery.modules.gallery.repositories.ImageRepository;
import ec.edu.espe.SecureFrameGallery.modules.steganography.dtos.QuarantineLogResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.steganography.entities.QuarantineLog;
import ec.edu.espe.SecureFrameGallery.modules.steganography.repositories.QuarantineLogRepository;
import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
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
@DisplayName("QuarantineServiceImpl — revisión manual de imágenes sospechosas")
class QuarantineServiceImplTest {

    @Mock
    private QuarantineLogRepository quarantineLogRepository;

    @Mock
    private ImageRepository imageRepository;

    @InjectMocks
    private QuarantineServiceImpl quarantineService;

    private Image suspiciousImage;
    private QuarantineLog quarantineLog;
    private User supervisor;

    @BeforeEach
    void setUp() {
        supervisor = new User();
        supervisor.setId(UUID.randomUUID());
        supervisor.setEmail("supervisor@espe.edu.ec");
        supervisor.setRole(Role.SUPERVISOR);

        suspiciousImage = new Image();
        suspiciousImage.setId(UUID.randomUUID());
        suspiciousImage.setOriginalName("sospechosa.jpg");
        suspiciousImage.setImageStatus(ImageStatus.QUARANTINED);

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
        when(quarantineLogRepository.findBySupervisorDecisionIsNull())
            .thenReturn(List.of(quarantineLog));

        List<QuarantineLogResponseDto> result = quarantineService.getPendingQuarantine();

        assertEquals(1, result.size());
        assertEquals(suspiciousImage.getId(), result.get(0).getImageId());
    }

    @Test
    @DisplayName("Listar cuarentena vacía retorna lista vacía")
    void listQuarantineReturnsEmptyWhenClean() {
        when(quarantineLogRepository.findBySupervisorDecisionIsNull()).thenReturn(List.of());
        assertTrue(quarantineService.getPendingQuarantine().isEmpty());
    }

    @Test
    @DisplayName("Aprobar imagen en cuarentena cambia estado a APPROVED")
    void approveQuarantinedImageChangesStatusToApproved() {
        when(quarantineLogRepository.findById(quarantineLog.getId()))
            .thenReturn(Optional.of(quarantineLog));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quarantineLogRepository.save(any(QuarantineLog.class))).thenAnswer(inv -> inv.getArgument(0));

        quarantineService.approveImage(quarantineLog.getId(), supervisor, "Falso positivo");

        assertEquals(ImageStatus.APPROVED, suspiciousImage.getImageStatus());
        assertEquals("APPROVED", quarantineLog.getSupervisorDecision());
        verify(imageRepository).save(suspiciousImage);
    }

    @Test
    @DisplayName("Rechazar imagen en cuarentena cambia estado a REJECTED")
    void rejectQuarantinedImageChangesStatusToRejected() {
        when(quarantineLogRepository.findById(quarantineLog.getId()))
            .thenReturn(Optional.of(quarantineLog));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quarantineLogRepository.save(any(QuarantineLog.class))).thenAnswer(inv -> inv.getArgument(0));

        quarantineService.rejectImage(quarantineLog.getId(), supervisor, "Confirmado");

        assertEquals(ImageStatus.REJECTED, suspiciousImage.getImageStatus());
        assertEquals("REJECTED", quarantineLog.getSupervisorDecision());
    }

    @Test
    @DisplayName("Aprobar log inexistente lanza excepción")
    void approveNonexistentLogThrows() {
        UUID fakeId = UUID.randomUUID();
        when(quarantineLogRepository.findById(fakeId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
            () -> quarantineService.approveImage(fakeId, supervisor, "notas"));
    }

    @Test
    @DisplayName("Registrar imagen sospechosa guarda log con razón de detección")
    void quarantineImageSavesLogWithDetectionReason() {
        String reason = "Chi-square p-value: 0.93";
        when(quarantineLogRepository.save(any(QuarantineLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));

        quarantineService.quarantineImage(suspiciousImage, reason, 0.93, false);

        verify(quarantineLogRepository).save(argThat(log ->
            log.getDetectionReason().equals(reason) &&
            log.getImage().getId().equals(suspiciousImage.getId())
        ));
        assertEquals(ImageStatus.QUARANTINED, suspiciousImage.getImageStatus());
    }

    @Test
    @DisplayName("Log de cuarentena guarda el score LSB recibido")
    void quarantineLogStoresLsbScore() {
        when(quarantineLogRepository.save(any(QuarantineLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));

        quarantineService.quarantineImage(suspiciousImage, "Entropía alta", 0.998, true);

        verify(quarantineLogRepository).save(argThat(log ->
            log.getLsbScore() == 0.998 && Boolean.TRUE.equals(log.getEofAnomaly())
        ));
    }
}
