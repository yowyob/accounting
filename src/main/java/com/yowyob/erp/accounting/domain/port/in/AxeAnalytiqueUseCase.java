package com.yowyob.erp.accounting.domain.port.in;

import com.yowyob.erp.accounting.domain.model.AxeAnalytique;
import com.yowyob.erp.accounting.infrastructure.web.dto.AxeAnalytiqueDto;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the AxeAnalytique operations.
 */
public interface AxeAnalytiqueUseCase {
    Mono<AxeAnalytiqueDto> create(AxeAnalytiqueDto dto);
    Mono<AxeAnalytiqueDto> update(UUID id, AxeAnalytiqueDto dto);
    Mono<Void> delete(UUID id);
    Mono<AxeAnalytiqueDto> findById(UUID id);
    Flux<AxeAnalytiqueDto> getAll();
    Flux<AxeAnalytiqueDto> getActive();
}
