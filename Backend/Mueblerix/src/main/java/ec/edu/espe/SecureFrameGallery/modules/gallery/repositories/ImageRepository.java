package ec.edu.espe.SecureFrameGallery.modules.gallery.repositories;

import ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Album;
import ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Image;
import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para imágenes.
 * RF03, RF04 — Consultas para el pipeline de análisis y la cola de cuarentena.
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {

    /** Imágenes de un álbum con un estado específico. */
    List<Image> findByAlbumAndImageStatus(Album album, ImageStatus status);

    /** Imágenes de un álbum en cualquiera de los estados indicados (p. ej. visibles: CLEAN/APPROVED). */
    List<Image> findByAlbumAndImageStatusIn(Album album, List<ImageStatus> statuses);

    /** Imágenes de un álbum (independientemente del estado). */
    List<Image> findByAlbum(Album album);

    /** Todas las imágenes en cuarentena (para el supervisor). */
    List<Image> findByImageStatus(ImageStatus status);
}
