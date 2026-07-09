package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.RegleValorisationStock;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface RegleValorisationStockRepository extends R2dbcRepository<RegleValorisationStock, UUID> {
    Flux<RegleValorisationStock> findByOrganizationId(UUID organizationId);
}
