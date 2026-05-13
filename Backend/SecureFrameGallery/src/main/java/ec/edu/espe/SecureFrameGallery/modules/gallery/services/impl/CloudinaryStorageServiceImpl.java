package ec.edu.espe.SecureFrameGallery.modules.gallery.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import ec.edu.espe.SecureFrameGallery.application.exceptions.BusinessException;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.CloudinaryStorageService;
import ec.edu.espe.SecureFrameGallery.modules.gallery.services.CloudinaryUploadResult;
import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación de CloudinaryStorageService.
 * 
 * Responsabilidades:
 * 1. Gestionar carpetas según estado de imagen (approved, quarantine, temp)
 * 2. Centralizar lógica de upload, delete y movimiento de imágenes
 * 3. Abstraer detalles técnicos de Cloudinary
 * 4. Mantener estructura escalable y mantenible
 * 
 * Patrón: Facade sobre Cloudinary SDK
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements CloudinaryStorageService {
    
    private final Cloudinary cloudinary;
    
    private static final String ROOT_FOLDER = "secure-gallery";
    private static final String APPROVED_FOLDER = "approved";
    private static final String QUARANTINE_FOLDER = "quarantine";
    private static final String TEMP_FOLDER = "temp";
    
    @Override
    public CloudinaryUploadResult uploadImage(
        byte[] imageBytes,
        UUID albumId,
        ImageStatus imageStatus,
        String filename
    ) {
        try {
            String folder = getFolderForStatus(imageStatus, albumId);
            log.debug("Subiendo imagen a Cloudinary - folder: {}, album: {}, estado: {}", 
                folder, albumId, imageStatus);
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                imageBytes,
                ObjectUtils.asMap(
                    "folder", folder,                    // Carpeta organizada
                    "resource_type", "image",            // Tipo de recurso
                    "quality", "auto",                   // Optimizar calidad automáticamente
                    "fetch_format", "auto",              // Formato óptimo por navegador
                    // Cloudinary Java SDK espera una LISTA de Transformation para eager
                    "eager", List.of(
                        new Transformation<>()
                            .width(800)
                            .crop("scale")
                            .quality("auto")
                            .fetchFormat("auto")
                    )
                )
            );
            
            // Extraer datos importantes
            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            
            // Validar resultado
            if (secureUrl == null || publicId == null) {
                log.error("Respuesta incompleta de Cloudinary para album: {}", albumId);
                throw new BusinessException(
                    "La respuesta de Cloudinary no contiene URL o public_id",
                    "CLOUDINARY_INVALID_RESPONSE",
                    HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            
            log.info("Imagen subida exitosamente - public_id: {}, folder: {}", publicId, folder);
            
            return new CloudinaryUploadResult(secureUrl, publicId, folder);
            
        } catch (Exception e) {
            log.error("Error al subir imagen a Cloudinary - album: {}, error: {}", 
                albumId, e.getMessage(), e);
            throw new BusinessException(
                "No fue posible almacenar la imagen en la nube",
                "CLOUDINARY_UPLOAD_FAILED",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    @Override
    public CloudinaryUploadResult moveImage(
        String publicId,
        ImageStatus currentStatus,
        ImageStatus newStatus,
        UUID albumId
    ) {
        try {
            log.info("Moviendo imagen - public_id: {}, de: {} a: {}", 
                publicId, currentStatus, newStatus);
            
            // Construir rutas de carpetas
            String currentFolder = getFolderForStatus(currentStatus, albumId);
            String newFolder = getFolderForStatus(newStatus, albumId);
            
            // Si la carpeta es la misma, no hacer nada
            if (currentFolder.equals(newFolder)) {
                log.debug("Imagen ya está en la carpeta destino: {}", newFolder);
                return new CloudinaryUploadResult(buildSecureUrl(publicId), publicId, newFolder);
            }

            String newPublicId = buildPublicIdInFolder(publicId, newFolder);

            log.debug("Renombrando en Cloudinary - from: {}, to: {}", publicId, newPublicId);
            cloudinary.uploader().rename(
                publicId,
                newPublicId,
                ObjectUtils.asMap(
                    "resource_type", "image",
                    "overwrite", true,
                    "invalidate", true
                )
            );

            String newSecureUrl = buildSecureUrl(newPublicId);
            log.info("Imagen movida exitosamente - new_public_id: {}, folder: {}", newPublicId, newFolder);

            return new CloudinaryUploadResult(newSecureUrl, newPublicId, newFolder);
            
        } catch (Exception e) {
            log.error("Error al mover imagen - public_id: {}, error: {}", 
                publicId, e.getMessage(), e);
            throw new BusinessException(
                "No fue posible actualizar la ubicación de la imagen",
                "CLOUDINARY_MOVE_FAILED",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    @Override
    public boolean deleteImage(String publicId) {
        try {
            log.debug("Eliminando imagen de Cloudinary - public_id: {}", publicId);
            
            Map<?, ?> deleteResult = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap("resource_type", "image")
            );
            
            String result = (String) deleteResult.get("result");
            boolean deleted = "ok".equals(result);
            
            if (deleted) {
                log.info("Imagen eliminada exitosamente - public_id: {}", publicId);
            } else {
                log.warn("No se pudo eliminar imagen o no existe - public_id: {}", publicId);
            }
            
            return deleted;
            
        } catch (Exception e) {
            log.error("Error al eliminar imagen - public_id: {}, error: {}", 
                publicId, e.getMessage(), e);
            // No lanzar excepción: el delete es mejor-esfuerzo
            return false;
        }
    }
    
    @Override
    public String getFolderForStatus(ImageStatus status, UUID albumId) {
        String statusFolder = mapStatusToFolder(status);
        return ROOT_FOLDER + "/" + statusFolder + "/" + albumId;
    }
    
    @Override
    public String buildSecureUrl(String publicId) {
        // Cloudinary construye URLs de forma determinista: https://res.cloudinary.com/{cloud}/image/upload/{public_id}
        return cloudinary.url()
            .type("upload")
            .resourceType("image")
            .secure(true)
            .generate(publicId);
    }
    
    // ==============================
    // HELPERS
    // ==============================
    
    /**
     * Mapea un ImageStatus a la carpeta correspondiente.
     * 
     * Lógica:
     * - CLEAN, APPROVED → approved/ (imagen aprobada)
     * - SUSPICIOUS, POSITIVE, QUARANTINED → quarantine/ (bajo revisión)
     * - PENDING, REJECTED → temp/ (temporal/rechazado)
     */
    private String mapStatusToFolder(ImageStatus status) {
        return switch (status) {
            case CLEAN, APPROVED -> APPROVED_FOLDER;
            case SUSPICIOUS, POSITIVE, QUARANTINED -> QUARANTINE_FOLDER;
            case PENDING, REJECTED -> TEMP_FOLDER;
        };
    }

    private String buildPublicIdInFolder(String currentPublicId, String newFolder) {
        if (currentPublicId == null || currentPublicId.isBlank()) {
            throw new BusinessException(
                "Public ID inválido para mover imagen",
                "CLOUDINARY_INVALID_PUBLIC_ID",
                HttpStatus.BAD_REQUEST
            );
        }
        String normalizedFolder = newFolder != null ? newFolder.replaceAll("/+$", "") : "";
        String baseName = currentPublicId;
        int lastSlash = currentPublicId.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < currentPublicId.length() - 1) {
            baseName = currentPublicId.substring(lastSlash + 1);
        }
        return normalizedFolder.isBlank() ? baseName : (normalizedFolder + "/" + baseName);
    }
}
