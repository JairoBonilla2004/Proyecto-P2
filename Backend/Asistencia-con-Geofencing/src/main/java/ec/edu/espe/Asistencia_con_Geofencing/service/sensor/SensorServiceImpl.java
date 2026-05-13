package ec.edu.espe.Asistencia_con_Geofencing.service.sensor;


import ec.edu.espe.Asistencia_con_Geofencing.dto.request.RegisterSensorEventRequest;
import ec.edu.espe.Asistencia_con_Geofencing.exception.ResourceNotFoundException;
import ec.edu.espe.Asistencia_con_Geofencing.model.SensorEvent;
import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.SensorType;
import ec.edu.espe.Asistencia_con_Geofencing.repository.SensorEventRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SensorServiceImpl implements SensorService {

    private final SensorEventRepository sensorEventRepository;
    private final UserRepository userRepository;

    @Override
    public UUID registerSensorEvent(UUID userId, RegisterSensorEventRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        SensorEvent event = new SensorEvent();
        event.setUser(user);
        event.setType(parseSensorType(request.getType()));
        event.setValue(request.getValue());
        event.setDeviceTime(request.getDeviceTime());
        sensorEventRepository.save(event);
        return event.getId();
    }

    private SensorType parseSensorType(String type) {
        try {
            return SensorType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Tipo de sensor no válido");
        }
    }
}
