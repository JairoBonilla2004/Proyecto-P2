package ec.edu.espe.Asistencia_con_Geofencing.service.session;


import ec.edu.espe.Asistencia_con_Geofencing.dto.mapper.SessionMapper;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.CreateSessionRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SessionResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SessionWithDistanceResponse;
import ec.edu.espe.Asistencia_con_Geofencing.exception.ResourceNotFoundException;
import ec.edu.espe.Asistencia_con_Geofencing.exception.UnauthorizedException;
import ec.edu.espe.Asistencia_con_Geofencing.model.AttendanceSession;
import ec.edu.espe.Asistencia_con_Geofencing.model.GeofenceZone;
import ec.edu.espe.Asistencia_con_Geofencing.model.Notification;
import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.RoleType;
import ec.edu.espe.Asistencia_con_Geofencing.repository.AttendanceSessionRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.GeofenceZoneRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.UserRepository;
import ec.edu.espe.Asistencia_con_Geofencing.service.geofence.GeofenceService;
import ec.edu.espe.Asistencia_con_Geofencing.service.notification.NotificationService;
import ec.edu.espe.Asistencia_con_Geofencing.service.notification.push.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final AttendanceSessionRepository sessionRepository;
    private final GeofenceZoneRepository geofenceZoneRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final GeofenceService geofenceService;

    @Override
    @Transactional
    public SessionResponse createSession(CreateSessionRequest request, UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado"));

        if (sessionRepository.existsByTeacherIdAndActiveTrue(teacherId)) {
            List<AttendanceSession> activeSessions = sessionRepository.findActiveSessionByTeacherId(teacherId);
            if (!activeSessions.isEmpty()) {
                AttendanceSession activeSession = activeSessions.get(0);
                throw new IllegalStateException(
                    String.format("Ya tienes una sesión activa: '%s'. Por favor, finaliza esa sesión antes de crear una nueva.", 
                        activeSession.getName())
                );
            }
        }

        GeofenceZone geofence = geofenceZoneRepository.findById(request.getGeofenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Zona de geofencing no encontrada"));

        AttendanceSession session = new AttendanceSession();
        session.setName(request.getName());
        session.setTeacher(teacher);
        session.setGeofence(geofence);
        session.setStartTime(request.getStartTime());
        session.setActive(true);
        session = sessionRepository.save(session);
        return SessionMapper.mapToResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions() {
        return sessionRepository.findByActiveTrue().stream()
                .map(SessionMapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SessionResponse> getMySessionsAsTeacher(UUID teacherId, Pageable pageable) {
        return sessionRepository.findByTeacherId(teacherId, pageable)
                .map(SessionMapper::mapToResponse);
    }

    @Override
    @Transactional
    public SessionResponse endSession(UUID sessionId, UUID teacherId) {

        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada"));

        if (!session.getTeacher().getId().equals(teacherId)) {
            throw new UnauthorizedException("No tienes permiso para finalizar esta sesión");
        }

        session.setActive(false);
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);

        List<Notification> notifications =
                notificationService.createAbsenceNotifications(session);

        notifications.stream()
                .filter(n -> n.getUser().hasRole(RoleType.STUDENT))
                .collect(Collectors.groupingBy(n -> n.getUser().getId()))
                .values()
                .forEach(userNotifications -> {
                    pushNotificationService.sendAbsencePush(userNotifications.get(0));
                });

        return SessionMapper.mapToResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionWithDistanceResponse> getActiveSessionsWithDistances(BigDecimal latitude, BigDecimal longitude) {
        List<AttendanceSession> activeSessions = sessionRepository.findByActiveTrue();
        
        return activeSessions.stream()
                .map(session -> {
                    GeofenceZone zone = session.getGeofence();
                    
                    // Calcular distancia desde la ubicación del estudiante a la zona
                    double distance = geofenceService.calculateDistance(
                            latitude,
                            longitude,
                            zone.getLatitude(),
                            zone.getLongitude()
                    );
                    
                    boolean withinZone = distance <= zone.getRadiusMeters();
                    
                    return SessionWithDistanceResponse.builder()
                            .sessionId(session.getId())
                            .name(session.getName())
                            .teacherName(session.getTeacher().getFullName())
                            .zoneName(zone.getName())
                            .zoneLatitude(zone.getLatitude().doubleValue())
                            .zoneLongitude(zone.getLongitude().doubleValue())
                            .radiusMeters(zone.getRadiusMeters())
                            .distanceInMeters(distance)
                            .withinZone(withinZone)
                            .qrToken(null) // No se expone el token en esta consulta por seguridad
                            .startTime(session.getStartTime())
                            .endTime(session.getEndTime())
                            .active(session.getActive())
                            .build();
                })
                .collect(Collectors.toList());
    }

}
