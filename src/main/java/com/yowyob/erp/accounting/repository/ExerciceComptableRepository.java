package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.ExerciceComptable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface ExerciceComptableRepository extends R2dbcRepository<ExerciceComptable, UUID> {

    @Query("SELECT * FROM exercices_comptables WHERE tenant_id = :tenantId")
    Flux<ExerciceComptable> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT * FROM exercices_comptables WHERE tenant_id = :tenantId AND code = :code")
    Mono<ExerciceComptable> findByTenantIdAndCode(@Param("tenantId") UUID tenantId, @Param("code") String code);

    @Query("SELECT * FROM exercices_comptables WHERE tenant_id = :tenantId AND :date BETWEEN date_debut AND date_fin")
    Mono<ExerciceComptable> findActiveForDate(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);
}
