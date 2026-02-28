package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.EcritureComptable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * R2DBC Repository interface for managing EcritureComptable entities.
 */
@Repository
public interface EcritureComptableRepository extends R2dbcRepository<EcritureComptable, UUID> {

      @Query("SELECT * FROM ecritures_comptables WHERE organization_id = :organization_id")
      Flux<EcritureComptable> findByOrganization_Id(@Param("organization_id") UUID organization_id);

      @Query("SELECT * FROM ecritures_comptables WHERE organization_id = :organization_id AND id = :id")
      Mono<EcritureComptable> findByOrganization_IdAndId(@Param("organization_id") UUID organization_id,
                  @Param("id") UUID id);

      @Query("SELECT * FROM ecritures_comptables WHERE organization_id = :organization_id AND validee = false")
      Flux<EcritureComptable> findByOrganization_IdAndValideeFalse(@Param("organization_id") UUID organization_id);

      @Query("SELECT * FROM ecritures_comptables WHERE organization_id = :organization_id AND date_ecriture BETWEEN :start_date AND :end_date")
      Flux<EcritureComptable> findByOrganization_IdAndDate_ecritureBetween(
                  @Param("organization_id") UUID organization_id,
                  @Param("start_date") LocalDate start_date,
                  @Param("end_date") LocalDate end_date);

      @Query("SELECT * FROM ecritures_comptables WHERE organization_id = :organization_id AND journal_id = :journal_id")
      Flux<EcritureComptable> findByOrganization_IdAndJournal_Id(
                  @Param("organization_id") UUID organization_id,
                  @Param("journal_id") UUID journal_id);

      @Query("SELECT * FROM ecritures_comptables " +
                  "WHERE organization_id = :organization_id " +
                  "AND journal_id = :journal_id " +
                  "AND date_ecriture BETWEEN :start_date AND :end_date")
      Flux<EcritureComptable> findByOrganization_IdAndJournal_IdAndDate_ecritureBetween(
                  @Param("organization_id") UUID organization_id,
                  @Param("journal_id") UUID journal_id,
                  @Param("start_date") LocalDate start_date,
                  @Param("end_date") LocalDate end_date);

      @Query("SELECT * FROM ecritures_comptables " +
                  "WHERE organization_id = :organization_id " +
                  "AND date_ecriture BETWEEN :start_date AND :end_date " +
                  "AND validee = false")
      Flux<EcritureComptable> findNonValidatedByDateRange(
                  @Param("organization_id") UUID organization_id,
                  @Param("start_date") LocalDate start_date,
                  @Param("end_date") LocalDate end_date);

      @Query("SELECT COUNT(*) FROM ecritures_comptables " +
                  "WHERE organization_id = :organization_id " +
                  "AND periode_id = :periode_id " +
                  "AND validee = false")
      Mono<Long> countNonValidatedByPeriod(
                  @Param("organization_id") UUID organization_id,
                  @Param("periode_id") UUID periode_id);

      @Query("DELETE FROM ecritures_comptables WHERE organization_id = :organization_id")
      Mono<Void> deleteAllByOrganizationId(@Param("organization_id") UUID organization_id);
}
