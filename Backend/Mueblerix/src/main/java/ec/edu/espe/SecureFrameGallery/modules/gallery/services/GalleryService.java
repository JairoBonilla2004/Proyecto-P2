package ec.edu.espe.SecureFrameGallery.modules.gallery.services;

import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumCreateDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.ImageUploadResponse;
import ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Album;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface GalleryService {

    AlbumResponseDto createAlbum(AlbumCreateDto dto, User owner);

    List<AlbumResponseDto> getPublicApprovedAlbums();

    List<AlbumResponseDto> getMyAlbums(User owner);

    Album findAlbumOrThrow(UUID albumId);
    ImageUploadResponse uploadImage(MultipartFile file, UUID albumId, User uploader);

    List<ImageUploadResponse> getCleanImagesForAlbum(UUID albumId, User requester);

    void approveAlbum(UUID albumId, User supervisor);

    void rejectAlbum(UUID albumId, User supervisor);

    List<AlbumResponseDto> getPendingAlbums();

    void approveImage(UUID imageId, User supervisor, String notes);

    void rejectImage(UUID imageId, User supervisor, String notes);
}
