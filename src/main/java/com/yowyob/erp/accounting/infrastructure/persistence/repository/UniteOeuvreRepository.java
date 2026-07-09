package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.UniteOeuvre;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface UniteOeuvreRepository extends R2dbcRepository<UniteOeuvre, UUID> {
    Flux<UniteOeuvre> findByOrganizationId(UUID organizationId);
    Flux<UniteOeuvre> findByOrganizationIdAndActif(UUID organizationId, Boolean actif);
    Flux<UniteOeuvre> findByOrganizationIdAndCentreId(UUID organizationId, UUID centreId);
    Mono<UniteOeuvre> findByOrganizationIdAndCode(UUID organizationId, String code);
}
