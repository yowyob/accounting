package com.yowyob.erp.accounting.domain.port.in;

import com.yowyob.erp.accounting.domain.model.LignePaie;
import com.yowyob.erp.accounting.infrastructure.web.dto.LignePaieDto;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the Paie operations.
 */
public interface PaieUseCase {
    Mono<LignePaieDto> create(LignePaieDto dto);
    Mono<LignePaieDto> update(UUID id, LignePaieDto dto);
    Mono<Void> delete(UUID id);
    Mono<LignePaieDto> findById(UUID id);
    Flux<LignePaieDto> findByExercice(UUID exerciceId);
    Flux<LignePaieDto> findByPeriode(UUID periodeId);
    Mono<LignePaieDto> comptabiliser(UUID id);
}
