package ec.edu.espe.SecureFrameGallery.modules.gallery.repositories;

import ec.edu.espe.SecureFrameGallery.modules.gallery.entities.Album;
import ec.edu.espe.SecureFrameGallery.modules.auth.entities.User;
import ec.edu.espe.SecureFrameGallery.shared.enums.AlbumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface AlbumRepository extends JpaRepository<Album, UUID> {

    boolean existsByOwnerAndTitleIgnoreCase(User owner, String title);
    List<Album> findByApprovalStatusAndIsPublicTrue(AlbumStatus status);
    List<Album> findByOwner(User owner);
    List<Album> findByApprovalStatus(AlbumStatus status);
}
