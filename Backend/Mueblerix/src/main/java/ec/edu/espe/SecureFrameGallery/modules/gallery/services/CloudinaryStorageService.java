package ec.edu.espe.SecureFrameGallery.modules.gallery.services;

import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
import java.util.UUID;

public interface CloudinaryStorageService {

    CloudinaryUploadResult uploadImage(
        byte[] imageBytes,
        UUID albumId,
        ImageStatus imageStatus,
        String filename
    );

    CloudinaryUploadResult moveImage(
        String publicId,
        ImageStatus currentStatus,
        ImageStatus newStatus,
        UUID albumId
    );

    boolean deleteImage(String publicId);

    String getFolderForStatus(ImageStatus status, UUID albumId);

    String buildSecureUrl(String publicId);
}
