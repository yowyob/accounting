package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.LignePaie;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface LignePaieRepository extends R2dbcRepository<LignePaie, UUID> {

    Flux<LignePaie> findByOrganizationId(UUID organizationId);

    Flux<LignePaie> findByOrganizationIdAndExerciceId(UUID organizationId, UUID exerciceId);

    @Query("SELECT * FROM lignes_paie WHERE organization_id = :orgId AND periode_id = :periodeId")
    Flux<LignePaie> findByOrganizationIdAndPeriodeId(
        @Param("orgId") UUID orgId, @Param("periodeId") UUID periodeId);

    @Query("SELECT * FROM lignes_paie WHERE organization_id = :orgId AND statut = :statut")
    Flux<LignePaie> findByOrganizationIdAndStatut(
        @Param("orgId") UUID orgId, @Param("statut") String statut);

    @Query("SELECT * FROM lignes_paie WHERE organization_id = :orgId AND id = :id")
    Mono<LignePaie> findByOrganizationIdAndId(@Param("orgId") UUID orgId, @Param("id") UUID id);
}
