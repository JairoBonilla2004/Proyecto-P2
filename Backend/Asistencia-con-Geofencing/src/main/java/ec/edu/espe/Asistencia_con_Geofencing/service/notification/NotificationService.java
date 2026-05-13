package ec.edu.espe.Asistencia_con_Geofencing.service.notification;


import ec.edu.espe.Asistencia_con_Geofencing.dto.response.NotificationResponse;
import ec.edu.espe.Asistencia_con_Geofencing.model.AttendanceSession;
import ec.edu.espe.Asistencia_con_Geofencing.model.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    List<NotificationResponse> getUnreadNotifications(UUID userId);
    void markAsRead(UUID notificationId, UUID userId);
    int markAllAsRead(UUID userId);
    List<Notification>  createAbsenceNotifications(AttendanceSession session);



}
