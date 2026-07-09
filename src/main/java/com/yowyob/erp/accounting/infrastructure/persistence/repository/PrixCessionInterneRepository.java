package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.PrixCessionInterne;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface PrixCessionInterneRepository extends R2dbcRepository<PrixCessionInterne, UUID> {
    Flux<PrixCessionInterne> findByOrganizationId(UUID organizationId);

    Flux<PrixCessionInterne> findByOrganizationIdAndCentreCedantId(UUID organizationId, UUID centreCedantId);

    Flux<PrixCessionInterne> findByOrganizationIdAndCentreBeneficiaireId(UUID organizationId, UUID centreBeneficiaireId);

    Mono<PrixCessionInterne> findByOrganizationIdAndCentreCedantIdAndCentreBeneficiaireIdAndPrestationLibelleIgnoreCase(
        UUID organizationId, UUID centreCedantId, UUID centreBeneficiaireId, String prestationLibelle);
}
