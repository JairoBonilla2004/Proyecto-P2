package ec.edu.espe.Asistencia_con_Geofencing.repository;

import ec.edu.espe.Asistencia_con_Geofencing.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}
