package ec.edu.espe.SecureFrameGallery.modules.steganography.mappers;

import ec.edu.espe.SecureFrameGallery.modules.steganography.dtos.QuarantineLogResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.steganography.entities.QuarantineLog;
import org.springframework.stereotype.Component;


@Component
public class QuarantineMapper {

    public QuarantineLogResponseDto toResponseDto(QuarantineLog entity) {
        if (entity == null) {
            return null;
        }
        
        return QuarantineLogResponseDto.builder()
            .id(entity.getId())
            .imageId(entity.getImage() != null ? entity.getImage().getId() : null)
            .imageName(entity.getImage() != null ? entity.getImage().getOriginalName() : null)
            .imageUrl(entity.getImage() != null ? entity.getImage().getStoredUrl() : null)
            .imageStatus(entity.getImage() != null ? entity.getImage().getImageStatus() : null)
            .detectionReason(entity.getDetectionReason())
            .lsbScore(entity.getLsbScore())
            .eofAnomaly(entity.isEofAnomaly())
            .reviewedByEmail(entity.getReviewedBy() != null ? entity.getReviewedBy().getEmail() : null)
            .reviewedAt(entity.getReviewedAt())
            .supervisorDecision(entity.getSupervisorDecision())
            .supervisorNotes(entity.getSupervisorNotes())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    public QuarantineLogResponseDto toResponseDtoLazy(QuarantineLog entity) {
        if (entity == null) {
            return null;
        }
        
        return QuarantineLogResponseDto.builder()
            .id(entity.getId())
            .imageId(entity.getImage() != null ? entity.getImage().getId() : null)
            .detectionReason(entity.getDetectionReason())
            .lsbScore(entity.getLsbScore())
            .eofAnomaly(entity.isEofAnomaly())
            .reviewedAt(entity.getReviewedAt())
            .supervisorDecision(entity.getSupervisorDecision())
            .supervisorNotes(entity.getSupervisorNotes())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
