package ec.edu.espe.SecureFrameGallery.modules.gallery.entities;

import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.shared.enums.AlbumStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA que representa un álbum fotográfico.
 * RF02 — Gestión de Álbumes: crear, listar, aprobar/rechazar (supervisor).
 */
@Entity
@Table(
        name = "albums",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"owner_id", "title"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    /**
     * Estado de aprobación del álbum por el supervisor.
     * Por defecto: PENDING_REVIEW al momento de creación.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private AlbumStatus approvalStatus = AlbumStatus.PENDING_REVIEW;

    /** true = visible al público (si también está APPROVED). */
    @Column(name = "public", nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Image> images = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
