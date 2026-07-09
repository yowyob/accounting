package com.yowyob.erp.accounting.domain.port.in;

import com.yowyob.erp.accounting.infrastructure.web.dto.CompteAnalytiqueDto;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CompteAnalytiqueUseCase {
    Mono<CompteAnalytiqueDto> create(CompteAnalytiqueDto dto);
    Mono<CompteAnalytiqueDto> update(UUID id, CompteAnalytiqueDto dto);
    Mono<Void> delete(UUID id);
    Mono<CompteAnalytiqueDto> findById(UUID id);
    Flux<CompteAnalytiqueDto> getAll();
    Flux<CompteAnalytiqueDto> getActive();
}
