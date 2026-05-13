package ec.edu.espe.Asistencia_con_Geofencing.repository;

import ec.edu.espe.Asistencia_con_Geofencing.model.QrToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QrTokenRepository extends JpaRepository<QrToken, UUID> {

    @Query("SELECT qt FROM QrToken qt WHERE qt.token = :token AND qt.expiresAt > :now")
    Optional<QrToken> findValidToken(String token, LocalDateTime now);

    @Query("UPDATE QrToken qt SET qt.expiresAt = :invalidTime WHERE qt.session.id = :sessionId AND qt.expiresAt > :now")
    @Modifying
    void invalidateSessionTokens(UUID sessionId, LocalDateTime now, LocalDateTime invalidTime);
}