package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.AxeAnalytique;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface AxeAnalytiqueRepository extends R2dbcRepository<AxeAnalytique, UUID> {

    Flux<AxeAnalytique> findByOrganizationId(UUID organizationId);

    Flux<AxeAnalytique> findByOrganizationIdAndActif(UUID organizationId, Boolean actif);

    Mono<AxeAnalytique> findByOrganizationIdAndCode(UUID organizationId, String code);

    @Query("SELECT compte_id FROM axe_analytique_comptes WHERE axe_id = :axeId")
    Flux<UUID> findLinkedCompteIds(@Param("axeId") UUID axeId);

    @Query("INSERT INTO axe_analytique_comptes (axe_id, compte_id) VALUES (:axeId, :compteId)")
    Mono<Void> linkCompte(@Param("axeId") UUID axeId, @Param("compteId") UUID compteId);

    @Query("DELETE FROM axe_analytique_comptes WHERE axe_id = :axeId")
    Mono<Void> unlinkAllComptes(@Param("axeId") UUID axeId);
}
