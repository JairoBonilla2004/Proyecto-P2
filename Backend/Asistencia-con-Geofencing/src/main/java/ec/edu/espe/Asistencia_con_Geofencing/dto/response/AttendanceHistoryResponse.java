package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceHistoryResponse {
    private AttendanceSummary summary;
    private List<AttendanceResponse> attendances;
    private Integer totalPages;
    private Integer currentPage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceSummary {
        private Long totalSessions;
        private Long attendedSessions;
        private Double attendanceRate;
    }
}