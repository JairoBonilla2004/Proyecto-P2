package ec.edu.espe.Asistencia_con_Geofencing.service.session;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.CreateSessionRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SessionResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SessionWithDistanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SessionService {
    SessionResponse createSession(CreateSessionRequest request, UUID teacherId);
    List<SessionResponse> getActiveSessions();
    Page<SessionResponse> getMySessionsAsTeacher(UUID teacherId, Pageable pageable);
    SessionResponse endSession(UUID sessionId, UUID teacherId);
    List<SessionWithDistanceResponse> getActiveSessionsWithDistances(BigDecimal latitude, BigDecimal longitude);

}
