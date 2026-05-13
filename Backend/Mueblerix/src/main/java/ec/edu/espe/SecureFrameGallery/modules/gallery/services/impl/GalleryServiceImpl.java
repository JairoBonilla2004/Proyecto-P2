package ec.edu.espe.SecureFrameGallery.modules.gallery.services.impl;

import ec.edu.espe.SecureFrameGallery.application.exceptions.BusinessException;
import ec.edu.espe.SecureFrameGallery.application.exceptions.InvalidStateException;
import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumCreateDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.AlbumResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.gallery.dtos.ImageUploadResponse;
import ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Album;
import ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Image;
import ec.edu.espe.SecureFrameGallery.modules.gallery.repositories.AlbumRepository;
import ec.edu.espe.SecureFrameGallery.modules.gallery.repositories.ImageRepository;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.CloudinaryStorageService;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.CloudinaryUploadResult;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.GalleryService;
import ec.edu.espe.SecureFrameGallery.modules.steganography.dtos.StegoAnalysisResultDto;
import ec.edu.espe.SecureFrameGallery.modules.steganography.entities.QuarantineLog;
import ec.edu.espe.SecureFrameGallery.modules.steganography.repositories.QuarantineLogRepository;
import ec.edu.espe.SecureFrameGallery.modules.steganography.services.ImageAnalyzer;
import ec.edu.espe.SecureFrameGallery.modules.steganography.services.MetadataCleaner;
import ec.edu.espe.SecureFrameGallery.shared.enums.AlbumStatus;
import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
import ec.edu.espe.SecureFrameGallery.shared.enums.Role;
import ec.edu.espe.SecureFrameGallery.shared.utils.MagicNumberUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GalleryServiceImpl implements GalleryService {

    private final AlbumRepository albumRepository;
    private final ImageRepository imageRepository;
    private final QuarantineLogRepository quarantineLogRepository;
    private final MagicNumberUtil magicNumberUtil;
    private final ImageAnalyzer imageAnalyzer;
    private final MetadataCleaner metadataCleaner;
    private final CloudinaryStorageService cloudinaryStorageService;



    @Override
    public AlbumResponseDto createAlbum(AlbumCreateDto dto, User owner) {
        String safeTitle = sanitizeHtml(dto.getTitle());
        String safeDesc  = dto.getDescription() != null ? sanitizeHtml(dto.getDescription()) : null;

        if (albumRepository.existsByOwnerAndTitleIgnoreCase(owner, safeTitle)) {
            throw new BusinessException(
                    "Ya tienes un álbum con ese título",
                    "ALBUM_DUPLICATED",
                    HttpStatus.CONFLICT
            );
        }

        Album album = Album.builder()
                .title(safeTitle)
                .description(safeDesc)
                .isPublic(dto.isPublic())
                .owner(owner)
                .approvalStatus(AlbumStatus.PENDING_REVIEW)
                .build();

        Album saved = albumRepository.save(album);
        log.info("Álbum creado: {} por {}", saved.getId(), owner.getEmail());
        return toAlbumDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlbumResponseDto> getPublicApprovedAlbums() {
        return albumRepository
                .findByApprovalStatusAndIsPublicTrue(AlbumStatus.APPROVED)
                .stream()
                .map(this::toAlbumDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlbumResponseDto> getMyAlbums(User owner) {
        return albumRepository.findByOwner(owner)
                .stream()
                .map(this::toAlbumDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Album findAlbumOrThrow(UUID albumId) {
        return albumRepository.findById(albumId)
                .orElseThrow(() -> new EntityNotFoundException("Álbum no encontrado: " + albumId));
    }

    @Override
    public void approveAlbum(UUID albumId, User supervisor) {
        Album album = findAlbumOrThrow(albumId);

        if (album.getApprovalStatus() == AlbumStatus.APPROVED) {
            throw new InvalidStateException("El álbum ya ha sido aprobado anteriormente");
        }
        if (album.getApprovalStatus() != AlbumStatus.PENDING_REVIEW) {
            throw new InvalidStateException(
                    "No se puede aprobar un álbum en estado: " + album.getApprovalStatus());
        }

        album.setApprovalStatus(AlbumStatus.APPROVED);
        albumRepository.save(album);
        log.info("Álbum {} aprobado por {}", albumId, supervisor.getEmail());
    }

    @Override
    public void rejectAlbum(UUID albumId, User supervisor) {
        Album album = findAlbumOrThrow(albumId);

        if (album.getApprovalStatus() == AlbumStatus.REJECTED) {
            throw new InvalidStateException("El álbum ya fue rechazado anteriormente");
        }

        album.setApprovalStatus(AlbumStatus.REJECTED);
        albumRepository.save(album);
        log.info("Álbum {} rechazado por {}", albumId, supervisor.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlbumResponseDto> getPendingAlbums() {
        return albumRepository.findByApprovalStatus(AlbumStatus.PENDING_REVIEW)
                .stream()
                .map(this::toAlbumDto)
                .collect(Collectors.toList());
    }


    @Override
    public void approveImage(UUID imageId, User supervisor, String notes) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Imagen no encontrada: " + imageId));

        if (image.getImageStatus() == ImageStatus.APPROVED) {
            throw new InvalidStateException("La imagen ya fue aprobada anteriormente");
        }
        if (image.getImageStatus() != ImageStatus.QUARANTINED) {
            throw new InvalidStateException(
                    "Solo se pueden aprobar imágenes en cuarentena. Estado actual: "
                            + image.getImageStatus());
        }

        if (image.getCloudinaryPublicId() != null && !image.getCloudinaryPublicId().isBlank()) {
            CloudinaryUploadResult moved = cloudinaryStorageService.moveImage(
                    image.getCloudinaryPublicId(),
                    ImageStatus.QUARANTINED,
                    ImageStatus.APPROVED,
                    image.getAlbum().getId()
            );
            image.setCloudinaryPublicId(moved.getPublicId());
            image.setStoredUrl(moved.getSecureUrl());
        }

        image.setImageStatus(ImageStatus.APPROVED);
        imageRepository.save(image);

        quarantineLogRepository.findByImage(image).ifPresent(qLog -> {
            qLog.setReviewedBy(supervisor);
            qLog.setReviewedAt(Instant.now());
            qLog.setSupervisorDecision(ImageStatus.APPROVED);
            qLog.setSupervisorNotes(notes);
            quarantineLogRepository.save(qLog);
        });

        log.info("Imagen {} aprobada por {}", imageId, supervisor.getEmail());
    }

    @Override
    public void rejectImage(UUID imageId, User supervisor, String notes) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Imagen no encontrada: " + imageId));

        if (image.getImageStatus() == ImageStatus.REJECTED) {
            throw new InvalidStateException("La imagen ya fue rechazada anteriormente");
        }
        if (image.getImageStatus() != ImageStatus.QUARANTINED) {
            throw new InvalidStateException(
                    "Solo se pueden rechazar imágenes en cuarentena. Estado actual: "
                            + image.getImageStatus());
        }

        if (image.getCloudinaryPublicId() != null && !image.getCloudinaryPublicId().isBlank()) {
            boolean deleted = cloudinaryStorageService.deleteImage(image.getCloudinaryPublicId());
            if (!deleted) {
                throw new BusinessException(
                        "No fue posible eliminar el archivo en la nube",
                        "CLOUDINARY_DELETE_FAILED",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            image.setCloudinaryPublicId(null);
            image.setStoredUrl(null);
        }

        image.setImageStatus(ImageStatus.REJECTED);
        imageRepository.save(image);

        quarantineLogRepository.findByImage(image).ifPresent(qLog -> {
            qLog.setReviewedBy(supervisor);
            qLog.setReviewedAt(Instant.now());
            qLog.setSupervisorDecision(ImageStatus.REJECTED);
            qLog.setSupervisorNotes(notes);
            quarantineLogRepository.save(qLog);
        });

        log.info("Imagen {} rechazada por {}", imageId, supervisor.getEmail());
    }

    @Override
    public ImageUploadResponse uploadImage(MultipartFile file, UUID albumId, User uploader) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    "El archivo está vacío o no fue enviado",
                    "FILE_EMPTY",
                    HttpStatus.BAD_REQUEST
            );
        }

        Album album = findAlbumOrThrow(albumId);

        boolean isOwner      = album.getOwner().getId().equals(uploader.getId());
        boolean isSupervisor = uploader.getRole() == Role.ROLE_SUPERVISOR;

        if (!isOwner && !isSupervisor) {
            throw new AccessDeniedException("No tienes permiso para subir imágenes a este álbum");
        }

        if (album.getApprovalStatus() != AlbumStatus.APPROVED && !isSupervisor) {
            throw new InvalidStateException(
                    "Solo puedes subir imágenes a un álbum aprobado. Estado actual: "
                            + album.getApprovalStatus());
        }

        String safeFilename = sanitizeFilename(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
        );

        try {
            byte[] rawBytes = file.getBytes();
            String detectedMimeType = magicNumberUtil.detectAndValidate(rawBytes);
            StegoAnalysisResultDto analysis = imageAnalyzer.analyze(rawBytes, detectedMimeType);
            if (analysis == null || analysis.getStatus() == null) {
                throw new BusinessException(
                        "No se pudo analizar la imagen",
                        "ANALYSIS_FAILED",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            byte[] cleanBytes = metadataCleaner.stripMetadata(rawBytes, detectedMimeType);

            // ── 4. Determinar estado final ─────────────────────────────────────
            // CLEAN permanece CLEAN (revisión manual no requerida).
            // SUSPICIOUS y POSITIVE van a QUARANTINED para revisión de supervisor.
            // Nota de diseño: imágenes CLEAN no pueden llegar a APPROVED
            // sin pasar por QUARANTINED. Si se requiere aprobación universal,
            // cambiar CLEAN → PENDING_REVIEW aquí.
            ImageStatus finalStatus = analysis.getStatus();
            if (finalStatus == ImageStatus.SUSPICIOUS || finalStatus == ImageStatus.POSITIVE) {
                finalStatus = ImageStatus.QUARANTINED;
            }

            // ── 5. Subir a Cloudinary ──────────────────────────────────────────
            CloudinaryUploadResult uploadResult = cloudinaryStorageService.uploadImage(
                    cleanBytes,
                    albumId,
                    finalStatus,
                    safeFilename
            );

            String cloudinaryUrl      = uploadResult.getSecureUrl();
            String cloudinaryPublicId = uploadResult.getPublicId();

            if (cloudinaryUrl == null || cloudinaryPublicId == null) {
                throw new BusinessException(
                        "Error al almacenar la imagen",
                        "CLOUDINARY_UPLOAD_FAILED",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

            // ── 6. Persistir entidad Image ─────────────────────────────────────
            Image image = Image.builder()
                    .originalName(safeFilename)
                    .storedUrl(cloudinaryUrl)
                    .cloudinaryPublicId(cloudinaryPublicId)
                    .mimeType(detectedMimeType)
                    .imageStatus(finalStatus)
                    .lsbEntropyScore(analysis.getLsbEntropyScore())
                    .analysisResult(
                            analysis.getDetectionReasons() != null
                                    ? String.join("; ", analysis.getDetectionReasons())
                                    : null
                    )
                    .album(album)
                    .uploadedBy(uploader)
                    .build();

            Image savedImage = imageRepository.save(image);

            // ── 7. Registrar en cuarentena si aplica ──────────────────────────
            if (savedImage.getImageStatus() == ImageStatus.QUARANTINED) {
                createQuarantineLog(savedImage, analysis, detectedMimeType);
            }

            log.info("Imagen subida: album={}, status={}, folder={}, mime={}",
                    albumId, finalStatus, uploadResult.getFolder(), detectedMimeType);

            return toImageResponse(savedImage);

        } catch (IOException e) {
            log.error("Error procesando imagen para album {}", albumId, e);
            throw new BusinessException(
                    "Error al procesar la imagen",
                    "IMAGE_PROCESSING_ERROR",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImageUploadResponse> getCleanImagesForAlbum(UUID albumId, User requester) {
        Album album = findAlbumOrThrow(albumId);

        boolean isOwner = requester != null && album.getOwner().getId().equals(requester.getId());
        boolean isSupervisor = requester != null && requester.getRole() == ec.edu.espe.SecureFrameGallery.shared.enums.Role.ROLE_SUPERVISOR;

        // Si no es dueño ni supervisor, validamos la privacidad
        if (!isOwner && !isSupervisor) {
            // El público solo puede ver imágenes si el álbum es público Y está aprobado
            if (!album.isPublic() || album.getApprovalStatus() != ec.edu.espe.SecureFrameGallery.shared.enums.AlbumStatus.APPROVED) {
                throw new org.springframework.security.access.AccessDeniedException("No tienes permiso para ver las imágenes de este álbum");
            }
        }

        // Si es dueño o supervisor, puede ver todas las imágenes de este álbum 
        // (incluyendo en cuarentena/rechazadas, según convenga. Aquí mostramos todas para el dueño).
        if (isOwner || isSupervisor) {
            return imageRepository.findByAlbum(album)
                    .stream()
                    .map(this::toImageResponse)
                    .collect(Collectors.toList());
        }

        // Para el público general, solo se muestran las imágenes limpias o aprobadas
        List<ImageStatus> visibleStatuses = Arrays.asList(ImageStatus.CLEAN, ImageStatus.APPROVED);
        return imageRepository.findByAlbumAndImageStatusIn(album, visibleStatuses)
                .stream()
                .map(this::toImageResponse)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Crea el registro de cuarentena incluyendo el mime type detectado
     * para facilitar la auditoría por parte del supervisor.
     */
    private void createQuarantineLog(Image image, StegoAnalysisResultDto analysis,
                                     String detectedMimeType) {
        String reasons = analysis.getDetectionReasons() != null
                ? String.join("; ", analysis.getDetectionReasons())
                : "No especificado";

        // El mime type detectado se añade al reason para trazabilidad completa
        String fullReason = String.format("[mime:%s] %s", detectedMimeType, reasons);

        QuarantineLog qLog = QuarantineLog.builder()
                .image(image)
                .detectionReason(fullReason)
                .lsbScore(analysis.getLsbEntropyScore())
                .eofAnomaly(analysis.isEofAnomalyFound())
                .build();

        quarantineLogRepository.save(qLog);
    }

    private AlbumResponseDto toAlbumDto(Album album) {
        int imageCount = album.getImages() != null ? album.getImages().size() : 0;
        return AlbumResponseDto.builder()
                .id(album.getId())
                .title(album.getTitle())
                .description(album.getDescription())
                .isPublic(album.isPublic())
                .approvalStatus(album.getApprovalStatus())
                .ownerEmail(album.getOwner().getEmail())
                .imageCount(imageCount)
                .createdAt(album.getCreatedAt())
                .build();
    }

    private ImageUploadResponse toImageResponse(Image image) {
        return ImageUploadResponse.builder()
                .id(image.getId())
                .originalName(image.getOriginalName())
                .storedUrl(image.getStoredUrl())
                .mimeType(image.getMimeType())
                .imageStatus(image.getImageStatus())
                .lsbEntropyScore(image.getLsbEntropyScore())
                .analysisResult(image.getAnalysisResult())
                .uploadedAt(image.getUploadedAt())
                .build();
    }

    /**
     * Elimina etiquetas HTML y caracteres peligrosos para prevenir XSS
     * en títulos y descripciones de álbumes.
     */
    private String sanitizeHtml(String input) {
        if (input == null) return null;
        return input
                .replaceAll("<[^>]*>", "")
                .replaceAll("&[a-zA-Z]+;", "")
                .replaceAll("[<>\"']", "")
                .trim();
    }

    /**
     * Normaliza el nombre de archivo para almacenamiento seguro.
     *
     * FIX: el substring se aplica sobre la cadena YA sanitizada, no sobre
     * el input original. En el código anterior, filename.length() podía
     * superar la longitud de la cadena sanitizada o producir un índice
     * incorrecto si los reemplazos cambiaban la longitud.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "image";
        String sanitized = filename
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("\\.{2,}", ".");
        return sanitized.substring(0, Math.min(sanitized.length(), 100));
    }
}