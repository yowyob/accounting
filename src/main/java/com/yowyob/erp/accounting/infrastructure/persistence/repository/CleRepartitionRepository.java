package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.CleRepartition;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface CleRepartitionRepository extends R2dbcRepository<CleRepartition, UUID> {
    Flux<CleRepartition> findByOrganizationId(UUID organizationId);
    Flux<CleRepartition> findByOrganizationIdAndActif(UUID organizationId, Boolean actif);
    Mono<CleRepartition> findByOrganizationIdAndCode(UUID organizationId, String code);
}
