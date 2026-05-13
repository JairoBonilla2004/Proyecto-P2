package ec.edu.espe.Asistencia_con_Geofencing.dto.mapper;

import ec.edu.espe.Asistencia_con_Geofencing.dto.response.NotificationResponse;
import ec.edu.espe.Asistencia_con_Geofencing.model.Notification;


public class NotificationMapper {

    public static NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .body(notification.getBody())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
