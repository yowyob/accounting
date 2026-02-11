package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.PlanComptable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive R2DBC Repository for PlanComptable entity.
 */
@Repository
public interface PlanComptableRepository extends R2dbcRepository<PlanComptable, UUID> {

        @Query("SELECT * FROM plans_comptables WHERE organization_id = :organization_id AND id = :id")
        Mono<PlanComptable> findByOrganization_IdAndId(@Param("organization_id") UUID organization_id, @Param("id") UUID id);

        @Query("SELECT * FROM plans_comptables WHERE organization_id = :organization_id")
        Flux<PlanComptable> findByOrganization_Id(@Param("organization_id") UUID organization_id);

        @Query("SELECT COUNT(*) > 0 FROM plans_comptables WHERE organization_id = :organization_id AND no_compte = :no_compte")
        Mono<Boolean> existsByOrganization_IdAndNo_compte(@Param("organization_id") UUID organization_id,
                        @Param("no_compte") String no_compte);

        @Query("SELECT * FROM plans_comptables WHERE organization_id = :organization_id AND no_compte = :no_compte")
        Mono<PlanComptable> findByOrganization_IdAndNo_compte(@Param("organization_id") UUID organization_id,
                        @Param("no_compte") String no_compte);

        @Query("SELECT * FROM plans_comptables WHERE organization_id = :organization_id AND actif = true")
        Flux<PlanComptable> findByOrganization_IdAndActifTrue(@Param("organization_id") UUID organization_id);

        @Query("SELECT * FROM plans_comptables WHERE organization_id = :organization_id AND no_compte LIKE CONCAT(:prefix, '%')")
        Flux<PlanComptable> findByOrganization_IdAndNo_compteStartingWith(@Param("organization_id") UUID organization_id,
                        @Param("prefix") String prefix);

        @Query("SELECT * FROM plans_comptables WHERE organization_id = :organization_id AND classe = :classe")
        Flux<PlanComptable> findByOrganization_IdAndClasse(@Param("organization_id") UUID organization_id,
                        @Param("classe") Integer classe);
}
