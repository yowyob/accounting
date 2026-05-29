package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.DeviseRepositoryPort;

import com.yowyob.erp.accounting.domain.model.Devise;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive R2DBC Repository for Devise entity.
 */
@Repository
public interface DeviseRepository extends R2dbcRepository<Devise, UUID>, DeviseRepositoryPort {

    Mono<Devise> findByCode(String code);

    Flux<Devise> findByActifTrue();

    Mono<Boolean> existsByCode(String code);
}
