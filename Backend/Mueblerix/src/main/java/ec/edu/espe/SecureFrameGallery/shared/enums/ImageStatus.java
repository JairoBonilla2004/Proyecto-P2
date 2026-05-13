package ec.edu.espe.SecureFrameGallery.shared.enums;


public enum ImageStatus {
    /** Imagen recién subida, aún no analizada. */
    PENDING,
    /** El análisis LSB y estructural no detectó anomalías. */
    CLEAN,
    /** El análisis detectó una puntuación de entropía elevada (posible ocultamiento). */
    SUSPICIOUS,
    /** El análisis detectó anomalías claras de esteganografía LSB o datos EOF. */
    POSITIVE,
    /** Imagen enviada a cuarentena; pendiente de revisión por supervisor. */
    QUARANTINED,
    /** Supervisor aprobó la imagen tras revisión. */
    APPROVED,
    /** Supervisor rechazó la imagen; no se muestra en la galería. */
    REJECTED
}
