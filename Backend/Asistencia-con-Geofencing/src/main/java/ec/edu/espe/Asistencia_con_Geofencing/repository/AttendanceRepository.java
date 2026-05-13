package ec.edu.espe.Asistencia_con_Geofencing.repository;

import ec.edu.espe.Asistencia_con_Geofencing.model.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    Optional<Attendance> findBySessionIdAndStudentId(UUID sessionId, UUID studentId);

    List<Attendance> findBySessionId(UUID sessionId);

    List<Attendance> findByStudentIdAndDeviceTimeBetween(
            UUID studentId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.deviceTime BETWEEN :startDate AND :endDate ORDER BY a.deviceTime DESC")
    Page<Attendance> findByStudentIdAndDateRange(UUID studentId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    List<Attendance> findByStudentIdAndIsSyncedFalse(UUID studentId);

    @Query("SELECT COUNT(DISTINCT a.session.id) FROM Attendance a WHERE a.student.id = :studentId")
    Long countAttendedSessionsByStudentId(UUID studentId);
}