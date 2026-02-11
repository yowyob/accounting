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

    @Query("SELECT * FROM exercices_comptables WHERE organization_id = :organizationId")
    Flux<ExerciceComptable> findByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("SELECT * FROM exercices_comptables WHERE organization_id = :organizationId AND code = :code")
    Mono<ExerciceComptable> findByOrganizationIdAndCode(@Param("organizationId") UUID organizationId, @Param("code") String code);

    @Query("SELECT * FROM exercices_comptables WHERE organization_id = :organizationId AND :date BETWEEN date_debut AND date_fin")
    Mono<ExerciceComptable> findActiveForDate(@Param("organizationId") UUID organizationId, @Param("date") LocalDate date);
}
