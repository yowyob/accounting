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

       @Query("SELECT * FROM periodes_comptables WHERE tenant_id = :tenant_id AND id = :id")
       Mono<PeriodeComptable> findByTenant_IdAndId(@Param("tenant_id") UUID tenant_id, @Param("id") UUID id);

       @Query("SELECT * FROM periodes_comptables WHERE tenant_id = :tenant_id ORDER BY date_debut DESC")
       Flux<PeriodeComptable> findByTenant_IdOrderByDate_debutDesc(@Param("tenant_id") UUID tenant_id);

       @Query("SELECT * FROM periodes_comptables WHERE tenant_id = :tenant_id AND code = :code")
       Mono<PeriodeComptable> findByTenant_IdAndCode(@Param("tenant_id") UUID tenant_id, @Param("code") String code);

       @Query("SELECT * FROM periodes_comptables WHERE tenant_id = :tenant_id AND :date BETWEEN date_debut AND date_fin")
       Mono<PeriodeComptable> findByTenant_IdAndDateInRange(@Param("tenant_id") UUID tenant_id,
                     @Param("date") LocalDate date);

       @Query("SELECT * FROM periodes_comptables WHERE tenant_id = :tenant_id AND cloturee = false")
       Flux<PeriodeComptable> findByTenant_IdAndClotureeFalse(@Param("tenant_id") UUID tenant_id);

       @Query("SELECT * FROM periodes_comptables WHERE tenant_id = :tenant_id AND date_debut >= :start_date AND date_fin <= :end_date")
       Flux<PeriodeComptable> findByTenant_IdAndPeriodRange(@Param("tenant_id") UUID tenant_id,
                     @Param("start_date") LocalDate start_date, @Param("end_date") LocalDate end_date);
}
