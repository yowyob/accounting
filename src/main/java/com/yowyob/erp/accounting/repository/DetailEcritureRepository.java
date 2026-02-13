package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.DetailEcriture;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * R2DBC Repository for managing DetailEcriture entities.
 */
@Repository
public interface DetailEcritureRepository extends R2dbcRepository<DetailEcriture, UUID> {

        Flux<DetailEcriture> findByOrganizationId(UUID organization_id);

        @Query("SELECT * FROM details_ecritures WHERE organization_id = :organization_id AND ecriture_id = :ecriture_id")
        Flux<DetailEcriture> findByOrganization_IdAndEcriture_Id(@Param("organization_id") UUID organization_id,
                        @Param("ecriture_id") UUID ecriture_id);

        @Query("SELECT * FROM details_ecritures WHERE organization_id = :organization_id AND compte_id = :compte_id")
        Flux<DetailEcriture> findByOrganization_IdAndCompte_Id(@Param("organization_id") UUID organization_id,
                        @Param("compte_id") UUID compte_id);

        @Query("SELECT * FROM details_ecritures WHERE organization_id = :organization_id AND date_ecriture BETWEEN :start_date AND :end_date")
        Flux<DetailEcriture> findByOrganization_IdAndDateRange(@Param("organization_id") UUID organization_id,
                        @Param("start_date") LocalDateTime start_date,
                        @Param("end_date") LocalDateTime end_date);

        @Query("SELECT COALESCE(SUM(montant_debit - montant_credit), 0) FROM details_ecritures WHERE organization_id = :organization_id AND compte_id = :compte_id")
        Mono<Double> calculateAccountBalance(@Param("organization_id") UUID organization_id,
                        @Param("compte_id") UUID compte_id);

        @Query("SELECT * FROM details_ecritures de " +
                        "WHERE de.organization_id = :organization_id " +
                        "AND COALESCE(de.pointee, false) = false " +
                        "AND ( " +
                        "(de.sens = 'DEBIT' AND de.montant_debit = :montant) OR " +
                        "(de.sens = 'CREDIT' AND de.montant_credit = :montant) " +
                        ") " +
                        "AND de.date_ecriture BETWEEN :date_debut AND :date_fin " +
                        "AND (LOWER(de.libelle) LIKE LOWER(CONCAT('%', :libelle, '%')) OR :libelle IS NULL) " +
                        "ORDER BY ABS(EXTRACT(DAY FROM (de.date_ecriture - :date_operation))) " +
                        "LIMIT 5")
        Flux<DetailEcriture> findCandidatesForPointage(
                        @Param("organization_id") UUID organization_id,
                        @Param("montant") BigDecimal montant,
                        @Param("date_debut") LocalDateTime date_debut,
                        @Param("date_fin") LocalDateTime date_fin,
                        @Param("libelle") String libelle,
                        @Param("date_operation") LocalDateTime date_operation);

        @Query("SELECT * FROM details_ecritures de " +
                        "WHERE de.organization_id = :organization_id " +
                        "AND COALESCE(de.pointee, false) = false " +
                        "AND ( " +
                        "(de.sens = 'DEBIT' AND de.montant_debit = :montant) OR " +
                        "(de.sens = 'CREDIT' AND de.montant_credit = :montant) " +
                        ") " +
                        "AND de.date_ecriture BETWEEN :date_debut AND :date_fin " +
                        "ORDER BY ABS(EXTRACT(DAY FROM (de.date_ecriture - :date_reference))) " +
                        "LIMIT 3")
        Flux<DetailEcriture> findByOrganizationIdAndMontantAndDateProche(
                        @Param("organization_id") UUID organization_id,
                        @Param("montant") BigDecimal montant,
                        @Param("date_debut") LocalDate date_debut,
                        @Param("date_fin") LocalDate date_fin,
                        @Param("date_reference") LocalDate date_reference);

        @Query("SELECT de.* FROM details_ecritures de " +
                        "JOIN comptes c ON de.compte_id = c.id " +
                        "WHERE de.organization_id = :organization_id " +
                        "AND c.no_compte IN (:account_numbers) " +
                        "AND de.date_ecriture BETWEEN :start_date AND :end_date")
        Flux<DetailEcriture> findByAccountNumbersAndDateRange(
                        @Param("organization_id") UUID organization_id,
                        @Param("account_numbers") java.util.Collection<String> account_numbers,
                        @Param("start_date") LocalDateTime start_date,
                        @Param("end_date") LocalDateTime end_date);

        @Query("DELETE FROM details_ecritures WHERE ecriture_id = :ecriture_id")
        Mono<Void> deleteByEcriture_id(@Param("ecriture_id") UUID ecriture_id);

}