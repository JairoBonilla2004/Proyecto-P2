package ec.edu.espe.Asistencia_con_Geofencing.service.notification;

import ec.edu.espe.Asistencia_con_Geofencing.dto.mapper.NotificationMapper;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.NotificationResponse;
import ec.edu.espe.Asistencia_con_Geofencing.exception.ResourceNotFoundException;
import ec.edu.espe.Asistencia_con_Geofencing.model.AttendanceSession;
import ec.edu.espe.Asistencia_con_Geofencing.model.Notification;
import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.NotificationType;
import ec.edu.espe.Asistencia_con_Geofencing.repository.AttendanceRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.NotificationRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        return notificationRepository.findUnreadByUserId(userId).stream()
                .map(NotificationMapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("No tienes permiso para marcar esta notificación");
        }

        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    @Transactional
    public List<Notification> createAbsenceNotifications(AttendanceSession session) {

        List<UUID> attendedStudentIds = attendanceRepository.findBySessionId(session.getId())
                .stream()
                .map(a -> a.getStudent().getId())
                .toList();

        List<User> absentStudents;
        if (attendedStudentIds.isEmpty()) {
            log.debug("No hay asistencias registradas, todos los estudiantes están ausentes");
            absentStudents = userRepository.findAllStudents();
        } else {
            log.debug("Buscando estudiantes ausentes excluyendo {} estudiantes que asistieron", attendedStudentIds.size());
            absentStudents = userRepository.findStudentsNotIn(attendedStudentIds);
        }

        List<Notification> notifications = absentStudents.stream()
                .map(student -> {
                    Notification notification = new Notification();
                    notification.setUser(student);
                    notification.setType(NotificationType.ABSENCE);
                    notification.setTitle("Ausencia no registrada");
                    notification.setBody(buildMessage(session));
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);
        log.info("Notificaciones de ausencia creadas: {}", notifications.size());
        return notifications;
    }

    private String buildMessage(AttendanceSession session) {
        return String.format(
                "No se registró tu asistencia en la sesión del %s con %s en %s",
                session.getStartTime().toLocalDate(),
                session.getTeacher().getFullName(),
                session.getGeofence().getName()
        );
    }
}
