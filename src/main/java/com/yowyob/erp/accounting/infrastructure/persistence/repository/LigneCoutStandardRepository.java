package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.LigneCoutStandard;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface LigneCoutStandardRepository extends R2dbcRepository<LigneCoutStandard, UUID> {
    Flux<LigneCoutStandard> findByFicheId(UUID ficheId);

    Mono<Void> deleteByFicheId(UUID ficheId);
}
