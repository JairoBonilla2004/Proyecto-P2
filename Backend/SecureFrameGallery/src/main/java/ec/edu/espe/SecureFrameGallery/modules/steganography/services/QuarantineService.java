package ec.edu.espe.SecureFrameGallery.modules.steganography.services;

import ec.edu.espe.SecureFrameGallery.modules.steganography.dtos.QuarantineLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;


public interface QuarantineService {

    List<QuarantineLogResponseDto> getPendingQuarantine();
    Page<QuarantineLogResponseDto> getPendingQuarantinePaged(Pageable pageable);
    List<QuarantineLogResponseDto> getReviewHistory();
    Page<QuarantineLogResponseDto> getReviewHistoryPaged(Pageable pageable);
    QuarantineLogResponseDto getQuarantineLogById(UUID id);
}
