package ec.edu.espe.SecureFrameGallery.modules.gallery.controllers;

import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.auth.repositories.UserRepository;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumCreateDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.GalleryService;
import ec.edu.espe.SecureFrameGallery.shared.dtos.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/albums")
@RequiredArgsConstructor
@Tag(name = "Álbumes", description = "Gestión de álbumes de la galería")
public class AlbumController {

    private final GalleryService galleryService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Listar álbumes públicos aprobados")
    public ResponseEntity<ApiResponse<List<AlbumResponseDto>>> getPublicAlbums() {
        List<AlbumResponseDto> albums = galleryService.getPublicApprovedAlbums();
        return ResponseEntity.ok(ApiResponse.ok(albums));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear nuevo álbum")
    public ResponseEntity<ApiResponse<AlbumResponseDto>> createAlbum(
            @Valid @RequestBody AlbumCreateDto dto,
            Authentication authentication) {
        User owner = resolveUser(authentication);
        AlbumResponseDto created = galleryService.createAlbum(dto, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Álbum creado", created));
    }

    @GetMapping("/mine")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar mis álbumes")
    public ResponseEntity<ApiResponse<List<AlbumResponseDto>>> getMyAlbums(Authentication authentication) {
        User owner = resolveUser(authentication);
        return ResponseEntity.ok(ApiResponse.ok(galleryService.getMyAlbums(owner)));
    }

    private User resolveUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }
}
