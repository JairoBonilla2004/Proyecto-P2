package ec.edu.espe.SecureFrameGallery.shared.enums;

/**
 * Roles de la aplicación.
 * RF01 — Gestión de Identidad y Roles.
 * - ROLE_USER: usuario estándar que puede subir imágenes y crear álbumes.
 * - ROLE_SUPERVISOR: puede revisar la cola de cuarentena y aprobar/rechazar contenido.
 */
public enum Role {
    ROLE_USER,
    ROLE_SUPERVISOR
}
