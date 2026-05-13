package ec.edu.espe.SecureFrameGallery.shared.enums;

/**
 * Estados del ciclo de vida de un Álbum.
 * RF02 — Gestión de Álbumes / RF05 — Visualización.
 *
 * Flujo de aprobación:
 *   PENDING_REVIEW → (supervisor) → APPROVED | REJECTED
 */
public enum AlbumStatus {
    /** Álbum creado, esperando revisión del supervisor. */
    PENDING_REVIEW,
    /** Álbum aprobado; visible en la galería pública. */
    APPROVED,
    /** Álbum rechazado; no se muestra públicamente. */
    REJECTED
}
