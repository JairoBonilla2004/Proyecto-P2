package ec.edu.espe.SecureFrameGallery.modules.steganography.dtos;

import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineLogResponseDto {
    
    private UUID id;
    private UUID imageId;
    
    private String imageName;
    private String imageUrl;
    private ImageStatus imageStatus;
    
    private String detectionReason;
    private Double lsbScore;
    private Boolean eofAnomaly;
    
    private String reviewedByEmail;
    private Instant reviewedAt;
    private ImageStatus supervisorDecision;
    private String supervisorNotes;
    
    private Instant createdAt;
}
