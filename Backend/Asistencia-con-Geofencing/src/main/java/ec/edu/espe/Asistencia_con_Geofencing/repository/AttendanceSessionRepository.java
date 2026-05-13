package ec.edu.espe.Asistencia_con_Geofencing.repository;

import ec.edu.espe.Asistencia_con_Geofencing.model.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {
    List<AttendanceSession> findByActiveTrue();
    long countByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
    @Query("SELECT s FROM AttendanceSession s WHERE s.teacher.id = :teacherId ORDER BY s.startTime DESC")
    Page<AttendanceSession> findByTeacherId(UUID teacherId, Pageable pageable);
    
    // Verificar si un profesor ya tiene una sesión activa
    boolean existsByTeacherIdAndActiveTrue(UUID teacherId);
    
    // Obtener la sesión activa de un profesor (si existe)
    @Query("SELECT s FROM AttendanceSession s WHERE s.teacher.id = :teacherId AND s.active = true")
    List<AttendanceSession> findActiveSessionByTeacherId(UUID teacherId);
}
