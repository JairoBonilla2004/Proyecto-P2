package ec.edu.espe.SecureFrameGallery.modules.gallery.dtos;

import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;


@Getter
@Builder
public class ImageUploadResponse {

    private UUID id;
    private String originalName;
    private String storedUrl;
    private String mimeType;
    private ImageStatus imageStatus;
    private Double lsbEntropyScore;
    private String analysisResult;
    private Instant uploadedAt;
}
