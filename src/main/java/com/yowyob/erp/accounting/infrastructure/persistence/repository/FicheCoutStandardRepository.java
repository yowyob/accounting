package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.FicheCoutStandard;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface FicheCoutStandardRepository extends R2dbcRepository<FicheCoutStandard, UUID> {
    Flux<FicheCoutStandard> findByOrganizationId(UUID organizationId);

    Flux<FicheCoutStandard> findByOrganizationIdAndPeriodeRefId(UUID organizationId, UUID periodeRefId);
}
