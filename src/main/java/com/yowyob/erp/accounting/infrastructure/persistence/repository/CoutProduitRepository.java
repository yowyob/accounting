package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.CoutProduit;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CoutProduitRepository extends R2dbcRepository<CoutProduit, UUID> {
    Flux<CoutProduit> findByOrganizationId(UUID organizationId);

    Flux<CoutProduit> findByOrganizationIdAndPeriodeId(UUID organizationId, UUID periodeId);

    Mono<CoutProduit> findByOrganizationIdAndProduitCodeIgnoreCaseAndPeriodeId(
        UUID organizationId, String produitCode, UUID periodeId);
}
