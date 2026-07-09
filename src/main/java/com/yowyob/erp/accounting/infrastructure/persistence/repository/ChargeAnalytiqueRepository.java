package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.ChargeAnalytique;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface ChargeAnalytiqueRepository extends R2dbcRepository<ChargeAnalytique, UUID> {
    Flux<ChargeAnalytique> findByOrganizationId(UUID organizationId);
    Flux<ChargeAnalytique> findByOrganizationIdAndPeriodeId(UUID organizationId, UUID periodeId);
    Flux<ChargeAnalytique> findByOrganizationIdAndType(UUID organizationId, String type);
}
