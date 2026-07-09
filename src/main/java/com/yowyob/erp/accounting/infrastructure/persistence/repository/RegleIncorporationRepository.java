package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.RegleIncorporation;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface RegleIncorporationRepository extends R2dbcRepository<RegleIncorporation, UUID> {
    Flux<RegleIncorporation> findByOrganizationId(UUID organizationId);

    Mono<RegleIncorporation> findByOrganizationIdAndCompteCgId(UUID organizationId, UUID compteCgId);

    @Query("""
        SELECT COUNT(*) > 0 FROM ecritures_analytiques ea
        JOIN details_ecritures de ON ea.ecriture_cg_ref = de.ecriture_id
        WHERE ea.organization_id = :orgId
          AND de.compte_id = :compteCgId
        """)
    Mono<Boolean> existsEcrituresForCompteCg(
        @Param("orgId") UUID orgId,
        @Param("compteCgId") UUID compteCgId);
}
