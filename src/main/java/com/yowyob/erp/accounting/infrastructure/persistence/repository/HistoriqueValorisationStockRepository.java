package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.HistoriqueValorisationStock;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface HistoriqueValorisationStockRepository extends R2dbcRepository<HistoriqueValorisationStock, UUID> {
    Flux<HistoriqueValorisationStock> findByRegleId(UUID regleId);
    Mono<Void> deleteByRegleId(UUID regleId);
}
