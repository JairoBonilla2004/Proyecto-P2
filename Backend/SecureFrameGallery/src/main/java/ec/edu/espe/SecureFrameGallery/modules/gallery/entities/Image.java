package ec.edu.espe.SecureFrameGallery.modules.gallery.entities;

import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa una imagen subida a un álbum.
 * RF03 — Subida de imágenes, análisis de esteganografía y eliminación de EXIF.
 */
@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Nombre original del archivo (sanitizado). */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** URL pública de Cloudinary (después de limpiar metadatos). */
    @Column(name = "stored_url", length = 512)
    private String storedUrl;

    /** Public ID de Cloudinary para operaciones de gestión (borrado, etc.). */
    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    /** MIME type detectado por magic bytes (no el del cliente). */
    @Column(name = "mime_type", nullable = false, length = 30)
    private String mimeType;

    /**
     * Estado del análisis de seguridad.
     * Flujo: PENDING → CLEAN | SUSPICIOUS | POSITIVE → QUARANTINED → APPROVED | REJECTED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "image_status", nullable = false, length = 20)
    @Builder.Default
    private ImageStatus imageStatus = ImageStatus.PENDING;

    /** Puntuación de entropía LSB del análisis (0.0 a 1.0). */
    @Column(name = "lsb_entropy_score")
    private Double lsbEntropyScore;

    /** Resumen del resultado del análisis para auditoría. */
    @Column(name = "analysis_result", length = 500)
    private String analysisResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private Instant uploadedAt;
}
