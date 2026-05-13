package ec.edu.espe.Asistencia_con_Geofencing.service.qr;


import ec.edu.espe.Asistencia_con_Geofencing.dto.request.GenerateQrRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.QrTokenResponse;

import java.util.UUID;

public interface QrService {

    QrTokenResponse generateQrToken(GenerateQrRequest request, UUID teacherId);
}
