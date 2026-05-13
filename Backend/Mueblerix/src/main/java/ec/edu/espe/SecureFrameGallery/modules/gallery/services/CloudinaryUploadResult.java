package ec.edu.espe.SecureFrameGallery.modules.gallery.services;

import lombok.Getter;

/**
 * DTO para encapsular resultado de upload a Cloudinary.
 * Simplifica el manejo de respuestas y reduce acoplamiento.
 */
@Getter
public class CloudinaryUploadResult {
    private final String secureUrl;
    private final String publicId;
    private final String folder;
    
    public CloudinaryUploadResult(String secureUrl, String publicId, String folder) {
        this.secureUrl = secureUrl;
        this.publicId = publicId;
        this.folder = folder;
    }
    
    @Override
    public String toString() {
        return "CloudinaryUploadResult{" +
                "publicId='" + publicId + '\'' +
                ", folder='" + folder + '\'' +
                '}';
    }
}
