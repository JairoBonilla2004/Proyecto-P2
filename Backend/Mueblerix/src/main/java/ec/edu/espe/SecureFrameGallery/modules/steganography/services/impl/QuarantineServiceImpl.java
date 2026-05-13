package ec.edu.espe.SecureFrameGallery.modules.steganography.services.impl;

import ec.edu.espe.SecureFrameGallery.modules.steganography.dtos.QuarantineLogResponseDto;
import ec.edu.espe.SecureFrameGallery.modules.steganography.entities.QuarantineLog;
import ec.edu.espe.SecureFrameGallery.modules.steganography.mappers.QuarantineMapper;
import ec.edu.espe.SecureFrameGallery.modules.steganography.repositories.QuarantineLogRepository;
import ec.edu.espe.SecureFrameGallery.modules.steganography.services.QuarantineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuarantineServiceImpl implements QuarantineService {
    
    private final QuarantineLogRepository repository;
    private final QuarantineMapper mapper;

    @Override
    public List<QuarantineLogResponseDto> getPendingQuarantine() {
        log.debug("Obteniendo registros pendientes de revisión de cuarentena");
        
        List<QuarantineLog> logs = repository.findPendingQuarantine();
        return logs.stream()
            .map(mapper::toResponseDto)
            .collect(Collectors.toList());
    }

    @Override
    public Page<QuarantineLogResponseDto> getPendingQuarantinePaged(Pageable pageable) {
        log.debug("Obteniendo registros pendientes (página: {}, tamaño: {})",
            pageable.getPageNumber(), pageable.getPageSize());
        Page<QuarantineLog> page = repository.findPendingQuarantinePaged(pageable);
        List<QuarantineLogResponseDto> dtos = page.getContent().stream()
            .map(mapper::toResponseDto)
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    public List<QuarantineLogResponseDto> getReviewHistory() {
        log.debug("Obteniendo historial completo de revisiones de cuarentena");
        List<QuarantineLog> logs = repository.findReviewHistory();
        return logs.stream()
            .map(mapper::toResponseDto)
            .collect(Collectors.toList());
    }

    @Override
    public Page<QuarantineLogResponseDto> getReviewHistoryPaged(Pageable pageable) {
        log.debug("Obteniendo historial de revisiones (página: {}, tamaño: {})",
            pageable.getPageNumber(), pageable.getPageSize());
        Page<QuarantineLog> page = repository.findReviewHistoryPaged(pageable);
        List<QuarantineLogResponseDto> dtos = page.getContent().stream()
            .map(mapper::toResponseDto)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    public QuarantineLogResponseDto getQuarantineLogById(UUID id) {
        log.debug("Obteniendo registro de cuarentena con ID: {}", id);
        return repository.findByIdWithRelations(id)
            .map(mapper::toResponseDto)
            .orElse(null);
    }
}
