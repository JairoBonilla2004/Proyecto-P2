package ec.edu.espe.SecureFrameGallery.modules.steganography.repositories;

import ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Image;
import ec.edu.espe.SecureFrameGallery.modules.steganography.entities.QuarantineLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface QuarantineLogRepository extends JpaRepository<QuarantineLog, UUID> {

    @Query("""
        SELECT q FROM QuarantineLog q
        JOIN FETCH q.image i
        LEFT JOIN FETCH q.reviewedBy
        WHERE q.reviewedBy IS NULL
        ORDER BY q.createdAt DESC
    """)
    List<QuarantineLog> findPendingQuarantine();

    @Query("""
        SELECT q FROM QuarantineLog q
        JOIN FETCH q.image i
        LEFT JOIN FETCH q.reviewedBy
        WHERE q.reviewedBy IS NULL
        ORDER BY q.createdAt DESC
    """)
    Page<QuarantineLog> findPendingQuarantinePaged(Pageable pageable);


    @Query("""
        SELECT q FROM QuarantineLog q
        JOIN FETCH q.image i
        JOIN FETCH q.reviewedBy
        WHERE q.reviewedBy IS NOT NULL
        ORDER BY q.reviewedAt DESC
    """)
    List<QuarantineLog> findReviewHistory();


    @Query("""
        SELECT q FROM QuarantineLog q
        JOIN FETCH q.image i
        JOIN FETCH q.reviewedBy
        WHERE q.reviewedBy IS NOT NULL
        ORDER BY q.reviewedAt DESC
    """)
    Page<QuarantineLog> findReviewHistoryPaged(Pageable pageable);


    @Query("""
        SELECT q FROM QuarantineLog q
        JOIN FETCH q.image
        LEFT JOIN FETCH q.reviewedBy
        WHERE q.image = :image
    """)
    Optional<QuarantineLog> findByImage(Image image);


    @Query("""
        SELECT q FROM QuarantineLog q
        JOIN FETCH q.image i
        LEFT JOIN FETCH q.reviewedBy
        WHERE q.id = :id
    """)
    Optional<QuarantineLog> findByIdWithRelations(UUID id);
}

