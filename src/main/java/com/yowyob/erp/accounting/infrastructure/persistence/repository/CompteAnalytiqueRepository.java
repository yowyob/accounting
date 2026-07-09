package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.port.out.CompteAnalytiqueRepositoryPort;
import com.yowyob.erp.accounting.domain.model.CompteAnalytique;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CompteAnalytiqueRepository extends R2dbcRepository<CompteAnalytique, UUID>, CompteAnalytiqueRepositoryPort {

    Flux<CompteAnalytique> findByOrganizationId(UUID organizationId);

    Flux<CompteAnalytique> findByOrganizationIdAndActif(UUID organizationId, Boolean actif);

    Mono<CompteAnalytique> findByOrganizationIdAndCode(UUID organizationId, String code);
}
