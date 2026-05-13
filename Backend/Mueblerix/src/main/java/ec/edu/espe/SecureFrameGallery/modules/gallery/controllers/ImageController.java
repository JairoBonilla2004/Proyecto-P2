package ec.edu.espe.SecureFrameGallery.modules.gallery.controllers;

import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.auth.repositories.UserRepository;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.ImageUploadResponse;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.GalleryService;
import ec.edu.espe.SecureFrameGallery.shared.dtos.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/albums/{albumId}/images")
@RequiredArgsConstructor
@Tag(name = "Imágenes", description = "Subida y visualización de imágenes")
public class ImageController {
    private final GalleryService galleryService;
    private final UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Subir imagen (activa pipeline de seguridad automáticamente)")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @PathVariable UUID albumId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        User uploader = resolveUser(authentication);
        ImageUploadResponse response = galleryService.uploadImage(file, albumId, uploader);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.ok("Imagen procesada correctamente", response)
        );
    }

    @GetMapping
    @Operation(summary = "Listar imágenes visibles del álbum (CLEAN/APPROVED)")
    public ResponseEntity<ApiResponse<List<ImageUploadResponse>>> getAlbumImages(@PathVariable UUID albumId) {
        
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        User requester = null;
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
             requester = userRepository.findByEmail(authentication.getName()).orElse(null);
        }

        List<ImageUploadResponse> images = galleryService.getCleanImagesForAlbum(albumId, requester);
        return ResponseEntity.ok(ApiResponse.ok(images));
    }

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
