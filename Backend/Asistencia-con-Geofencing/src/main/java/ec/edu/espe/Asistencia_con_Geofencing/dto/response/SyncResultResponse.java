package ec.edu.espe.Asistencia_con_Geofencing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResultResponse {
    private UUID batchId;
    private Integer syncedCount;
    private Integer failedCount;
    private List<SyncItemResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncItemResult {
        private String tempId;
        private UUID serverId;
        private String status; // "SYNCED" o "FAILED"
        private String message;
        private String errorCode;
    }
}
