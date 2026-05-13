package ec.edu.espe.Asistencia_con_Geofencing.service.notification.push;

import com.google.firebase.messaging.*;
import ec.edu.espe.Asistencia_con_Geofencing.model.Notification;
import ec.edu.espe.Asistencia_con_Geofencing.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final DeviceRepository deviceRepository;

    @Transactional
    public void sendAbsencePush(Notification notification){
        UUID userId = notification.getUser().getId();
        
        // Obtener TODOS los FCM tokens activos del usuario
        List<String> fcmTokens = deviceRepository.findAllActiveFcmTokensByUserId(userId);
        if (fcmTokens.isEmpty()) {
            log.warn("Usuario {} no tiene dispositivos activos con FCM token", userId);
            return;
        }
        
        log.info("Enviando notificación a usuario {} en {} dispositivo(s)", userId, fcmTokens.size());
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // Enviar a cada dispositivo
        fcmTokens.forEach(fcmToken -> {
            try {
                sendToDevice(notification, fcmToken);
                successCount.incrementAndGet();
            } catch (FirebaseMessagingException e) {
                failureCount.incrementAndGet();
                handlePushError(fcmToken, userId, e);
            } catch (Exception e) {
                failureCount.incrementAndGet();
                log.error("Error inesperado enviando push a token {}", 
                    fcmToken.substring(0, Math.min(20, fcmToken.length())), e);
            }
        });
        
        log.info("Notificación enviada a usuario {}: {} exitoso(s), {} fallido(s)", 
            userId, successCount.get(), failureCount.get());
    }

    private void sendToDevice(Notification notification, String fcmToken) throws FirebaseMessagingException {
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setTitle(notification.getTitle())
                        .setBody(notification.getBody())
                        .setChannelId("absence_notifications")
                        .setSound("default")
                        .setPriority(AndroidNotification.Priority.HIGH)
                        .setDefaultSound(true)
                        .build())
                .build();

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(
                        com.google.firebase.messaging.Notification.builder()
                                .setTitle(notification.getTitle())
                                .setBody(notification.getBody())
                                .build()
                )
                .setAndroidConfig(androidConfig)
                .putData("type", notification.getType().name())
                .putData("notificationId", notification.getId().toString())
                .build();

        String response = FirebaseMessaging.getInstance().send(message);
        log.debug("Push enviado exitosamente: {}", response);
    }

    private void handlePushError(String fcmToken, UUID userId, FirebaseMessagingException e) {
        String errorCode = e.getErrorCode() != null ? e.getErrorCode().toString() : "UNKNOWN";
        String tokenPreview = fcmToken.substring(0, Math.min(20, fcmToken.length()));
        
        // Errores que indican que el token ya no es válido
        if ("UNREGISTERED".equals(errorCode) || 
            "INVALID_ARGUMENT".equals(errorCode) ||
            "NOT_FOUND".equals(errorCode)) {
            
            log.warn("Token inválido detectado para usuario {} ({}...). Desactivando dispositivo automáticamente.", 
                userId, tokenPreview);
            
            try {
                deviceRepository.deactivateDeviceByFcmToken(fcmToken);
                log.info("Dispositivo desactivado automáticamente por token inválido");
            } catch (Exception ex) {
                log.error("Error al desactivar dispositivo con token inválido", ex);
            }
        } else {
            log.error("Error temporal enviando push a usuario {} ({}...): {}",
                userId, tokenPreview, e.getMessage());
        }
    }
}
