package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.ConfigurationAnalytique;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ConfigurationAnalytiqueRepository extends R2dbcRepository<ConfigurationAnalytique, UUID> {
    Mono<ConfigurationAnalytique> findByOrganizationId(UUID organizationId);
}
