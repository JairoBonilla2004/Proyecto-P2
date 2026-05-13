package ec.edu.espe.Asistencia_con_Geofencing.service.sensor;

import ec.edu.espe.Asistencia_con_Geofencing.dto.request.RegisterSensorEventRequest;
import java.util.UUID;

public interface SensorService {

    UUID registerSensorEvent(UUID userId, RegisterSensorEventRequest request);
}
