package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.ActiviteNormaleMethode;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ActiviteNormaleMethodeRepository extends R2dbcRepository<ActiviteNormaleMethode, UUID> {
    Flux<ActiviteNormaleMethode> findByMethodeCalculId(UUID methodeCalculId);
    Mono<Void> deleteByMethodeCalculId(UUID methodeCalculId);
}
