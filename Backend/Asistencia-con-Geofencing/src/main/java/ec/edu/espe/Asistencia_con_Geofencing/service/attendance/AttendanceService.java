package ec.edu.espe.Asistencia_con_Geofencing.service.attendance;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.SyncAttendancesRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.ValidateQrRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.AttendanceHistoryResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.AttendanceResponse;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.SyncResultResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public interface AttendanceService {
    AttendanceResponse validateQrAndRegisterAttendance(ValidateQrRequest request, UUID studentId);
    SyncResultResponse syncOfflineAttendances(SyncAttendancesRequest request, UUID userId);
    AttendanceHistoryResponse getMyHistory(UUID studentId, LocalDate startDate, LocalDate endDate, Pageable pageable);
    List<AttendanceResponse> getSessionAttendances(UUID sessionId);

}
