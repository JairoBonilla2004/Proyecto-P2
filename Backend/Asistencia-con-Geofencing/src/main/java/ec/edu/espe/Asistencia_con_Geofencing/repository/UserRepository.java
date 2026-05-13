package ec.edu.espe.Asistencia_con_Geofencing.repository;

import ec.edu.espe.Asistencia_con_Geofencing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("""
    SELECT u
    FROM User u
    JOIN u.roles r
    WHERE r.name = 'STUDENT'
      AND u.id NOT IN :attendedIds
    """)
    List<User> findStudentsNotIn(@Param("attendedIds") List<UUID> attendedIds);
    
    @Query("""
    SELECT u
    FROM User u
    JOIN u.roles r
    WHERE r.name = 'STUDENT'
    """)
    List<User> findAllStudents();

}
