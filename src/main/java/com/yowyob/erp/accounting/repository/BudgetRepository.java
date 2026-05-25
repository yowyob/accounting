package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Budget;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface BudgetRepository extends R2dbcRepository<Budget, UUID> {

    Flux<Budget> findByOrganizationId(UUID organizationId);

    Flux<Budget> findByOrganizationIdAndExerciceId(UUID organizationId, UUID exerciceId);

    Flux<Budget> findByOrganizationIdAndPeriodeId(UUID organizationId, UUID periodeId);

    Flux<Budget> findByOrganizationIdAndParentId(UUID organizationId, UUID parentId);

    Flux<Budget> findByOrganizationIdAndType(UUID organizationId, String type);

    @Query("SELECT * FROM budgets WHERE organization_id = :orgId AND exercice_id = :exerciceId AND compte_id = :compteId")
    Flux<Budget> findByOrgAndExerciceAndCompte(
        @Param("orgId") UUID orgId,
        @Param("exerciceId") UUID exerciceId,
        @Param("compteId") UUID compteId);

    @Query("DELETE FROM budgets WHERE organization_id = :orgId AND exercice_id = :exerciceId")
    Mono<Void> deleteByOrganizationIdAndExerciceId(@Param("orgId") UUID orgId, @Param("exerciceId") UUID exerciceId);

    @Query("SELECT axe_id FROM budget_axes WHERE budget_id = :budgetId")
    Flux<UUID> findLinkedAxeIds(@Param("budgetId") UUID budgetId);

    @Query("INSERT INTO budget_axes (budget_id, axe_id) VALUES (:budgetId, :axeId)")
    Mono<Void> linkAxe(@Param("budgetId") UUID budgetId, @Param("axeId") UUID axeId);

    @Query("DELETE FROM budget_axes WHERE budget_id = :budgetId")
    Mono<Void> unlinkAllAxes(@Param("budgetId") UUID budgetId);
}
