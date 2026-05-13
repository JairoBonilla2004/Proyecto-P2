package ec.edu.espe.Asistencia_con_Geofencing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Número de hilos que se mantienen activos
        executor.setCorePoolSize(5);

        // Número máximo de hilos
        executor.setMaxPoolSize(10);
        // Capacidad de la cola de espera
        executor.setQueueCapacity(100);
        // Prefijo del nombre de los hilos para identificarlos en logs
        executor.setThreadNamePrefix("async-report-");
        // Inicializar el executor
        executor.initialize();
        return executor;
    }
}