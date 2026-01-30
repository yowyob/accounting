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

        @Query("SELECT * FROM plans_comptables WHERE tenant_id = :tenant_id AND id = :id")
        Mono<PlanComptable> findByTenant_IdAndId(@Param("tenant_id") UUID tenant_id, @Param("id") UUID id);

        @Query("SELECT * FROM plans_comptables WHERE tenant_id = :tenant_id")
        Flux<PlanComptable> findByTenant_Id(@Param("tenant_id") UUID tenant_id);

        @Query("SELECT COUNT(*) > 0 FROM plans_comptables WHERE tenant_id = :tenant_id AND no_compte = :no_compte")
        Mono<Boolean> existsByTenant_IdAndNo_compte(@Param("tenant_id") UUID tenant_id,
                        @Param("no_compte") String no_compte);

        @Query("SELECT * FROM plans_comptables WHERE tenant_id = :tenant_id AND no_compte = :no_compte")
        Mono<PlanComptable> findByTenant_IdAndNo_compte(@Param("tenant_id") UUID tenant_id,
                        @Param("no_compte") String no_compte);

        @Query("SELECT * FROM plans_comptables WHERE tenant_id = :tenant_id AND actif = true")
        Flux<PlanComptable> findByTenant_IdAndActifTrue(@Param("tenant_id") UUID tenant_id);

        @Query("SELECT * FROM plans_comptables WHERE tenant_id = :tenant_id AND no_compte LIKE CONCAT(:prefix, '%')")
        Flux<PlanComptable> findByTenant_IdAndNo_compteStartingWith(@Param("tenant_id") UUID tenant_id,
                        @Param("prefix") String prefix);

        @Query("SELECT * FROM plans_comptables WHERE tenant_id = :tenant_id AND classe = :classe")
        Flux<PlanComptable> findByTenant_IdAndClasse(@Param("tenant_id") UUID tenant_id,
                        @Param("classe") Integer classe);
}
