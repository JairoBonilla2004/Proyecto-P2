package ec.edu.espe.Asistencia_con_Geofencing.model.enums;

/**
 * Estados posibles de un reporte
 * - GENERATING: El reporte está siendo generado
 * - COMPLETED: El reporte se generó exitosamente
 * - FAILED: Hubo un error al generar el reporte
 */
public enum ReportStatus {
    GENERATING,
    COMPLETED,
    FAILED
}
