package ec.edu.espe.Asistencia_con_Geofencing.repository;

import ec.edu.espe.Asistencia_con_Geofencing.model.SyncBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SyncBatchRepository extends JpaRepository<SyncBatch, UUID> {
}