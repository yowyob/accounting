package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.JournalAnalytique;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface JournalAnalytiqueRepository extends R2dbcRepository<JournalAnalytique, UUID> {
    Flux<JournalAnalytique> findByOrganizationId(UUID organizationId);
    Flux<JournalAnalytique> findByOrganizationIdAndActif(UUID organizationId, Boolean actif);
    Mono<JournalAnalytique> findByOrganizationIdAndCode(UUID organizationId, String code);
}
