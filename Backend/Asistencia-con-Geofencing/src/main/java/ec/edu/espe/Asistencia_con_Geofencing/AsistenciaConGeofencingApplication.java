package ec.edu.espe.Asistencia_con_Geofencing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AsistenciaConGeofencingApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsistenciaConGeofencingApplication.class, args);
	}

}
