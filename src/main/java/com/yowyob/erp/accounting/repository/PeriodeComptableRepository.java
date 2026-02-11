package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.PeriodeComptable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reactive R2DBC Repository for managing PeriodeComptable entities.
 */
@Repository
public interface PeriodeComptableRepository extends R2dbcRepository<PeriodeComptable, UUID> {

       @Query("SELECT * FROM periodes_comptables WHERE organization_id = :organization_id AND id = :id")
       Mono<PeriodeComptable> findByOrganization_IdAndId(@Param("organization_id") UUID organization_id, @Param("id") UUID id);

       @Query("SELECT * FROM periodes_comptables WHERE exercice_id = :exercice_id")
       Flux<PeriodeComptable> findByExerciceId(@Param("exercice_id") UUID exercice_id);

       @Query("SELECT * FROM periodes_comptables WHERE organization_id = :organization_id ORDER BY date_debut DESC")
       Flux<PeriodeComptable> findByOrganization_IdOrderByDate_debutDesc(@Param("organization_id") UUID organization_id);

       @Query("SELECT * FROM periodes_comptables WHERE organization_id = :organization_id AND code = :code")
       Mono<PeriodeComptable> findByOrganization_IdAndCode(@Param("organization_id") UUID organization_id, @Param("code") String code);

       @Query("SELECT * FROM periodes_comptables WHERE organization_id = :organization_id AND :date BETWEEN date_debut AND date_fin")
       Mono<PeriodeComptable> findByOrganization_IdAndDateInRange(@Param("organization_id") UUID organization_id,
                     @Param("date") LocalDate date);

       @Query("SELECT * FROM periodes_comptables WHERE organization_id = :organization_id AND cloturee = false")
       Flux<PeriodeComptable> findByOrganization_IdAndClotureeFalse(@Param("organization_id") UUID organization_id);

       @Query("SELECT * FROM periodes_comptables WHERE organization_id = :organization_id AND date_debut >= :start_date AND date_fin <= :end_date")
       Flux<PeriodeComptable> findByOrganization_IdAndPeriodRange(@Param("organization_id") UUID organization_id,
                     @Param("start_date") LocalDate start_date, @Param("end_date") LocalDate end_date);
}
