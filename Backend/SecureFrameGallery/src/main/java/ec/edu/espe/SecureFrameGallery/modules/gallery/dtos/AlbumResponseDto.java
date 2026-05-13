package ec.edu.espe.SecureFrameGallery.modules.gallery.dtos;

import ec.edu.espe.SecureFrameGallery.shared.enums.AlbumStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta para un álbum.
 * No expone entidades JPA directamente (previene serialización circular / over-fetching).
 */
@Getter
@Builder
public class AlbumResponseDto {

    private UUID id;
    private String title;
    private String description;
    private boolean isPublic;
    private AlbumStatus approvalStatus;
    private String ownerEmail;
    private int imageCount;
    private Instant createdAt;
}
