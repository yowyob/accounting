package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Devise;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive R2DBC Repository for Devise entity.
 */
@Repository
public interface DeviseRepository extends R2dbcRepository<Devise, UUID> {

    Mono<Devise> findByCode(String code);

    Flux<Devise> findByActifTrue();

    Mono<Boolean> existsByCode(String code);
}
