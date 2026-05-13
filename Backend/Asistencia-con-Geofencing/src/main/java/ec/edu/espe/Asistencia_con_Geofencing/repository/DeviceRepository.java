package ec.edu.espe.Asistencia_con_Geofencing.repository;

import ec.edu.espe.Asistencia_con_Geofencing.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findByUserIdAndDeviceIdentifier(UUID userId, String deviceIdentifier);
    
    List<Device> findByUserId(UUID userId);
    
    /**
     * Retorna TODOS los FCM tokens activos de un usuario
     * Solución profesional: enviar notificaciones a todos los dispositivos del usuario
     * (similar a WhatsApp, Gmail, Slack, etc.)
     */
        @Query(value = """
        SELECT d.fcm_token
        FROM devices d
        WHERE d.user_id = :userId
          AND d.fcm_token IS NOT NULL
          AND d.is_active = true
        ORDER BY d.last_seen DESC
    """, nativeQuery = true)
    List<String> findAllActiveFcmTokensByUserId(UUID userId);

    List<Device> findByDeviceIdentifierAndUserIdNot(String deviceIdentifier, UUID userId);
    
    /**
     * Desactiva un dispositivo por su FCM token (útil cuando Firebase reporta token inválido)
     */
    @Query("UPDATE Device d SET d.isActive = false WHERE d.fcmToken = :fcmToken")
    @org.springframework.data.jpa.repository.Modifying
    void deactivateDeviceByFcmToken(String fcmToken);
}