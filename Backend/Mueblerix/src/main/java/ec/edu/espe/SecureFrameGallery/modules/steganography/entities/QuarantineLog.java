package ec.edu.espe.SecureFrameGallery.modules.steganography.entities;

import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Image;
import ec.edu.espe.SecureFrameGallery.shared.enums.ImageStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "quarantine_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    /** Razón de detección (resumen del análisis). */
    @Column(name = "detection_reason", nullable = false, length = 1000)
    private String detectionReason;

    /** Puntuación LSB al momento de la detección. */
    @Column(name = "lsb_score")
    private Double lsbScore;

    /** Si se detectaron datos extra tras el EOF. */
    @Column(name = "eof_anomaly")
    private boolean eofAnomaly;

    // Revisión del supervisor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "supervisor_decision", length = 20)
    private ImageStatus supervisorDecision;

    @Column(name = "supervisor_notes", length = 500)
    private String supervisorNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
