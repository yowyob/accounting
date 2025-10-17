package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.EcritureComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EcritureComptableRepository extends JpaRepository<EcritureComptable, UUID> {

    /* ============================================================================
       🔹 Requêtes standards de base
    ============================================================================ */

    List<EcritureComptable> findByTenant_Id(UUID tenantId);

    Optional<EcritureComptable> findByTenant_IdAndId(UUID tenantId, UUID id);

    List<EcritureComptable> findByTenant_IdAndValideeFalse(UUID tenantId);


    /* ============================================================================
       🔎 Recherche par période et journal
    ============================================================================ */

    // ✅ Recherche par plage de dates uniquement
    List<EcritureComptable> findByTenant_IdAndDateEcritureBetween(UUID tenantId, LocalDate startDate, LocalDate endDate);

    // ✅ Recherche par journal uniquement
    List<EcritureComptable> findByTenant_IdAndJournal_Id(UUID tenantId, UUID journalId);

    // ✅ Recherche combinée : journal + période
    List<EcritureComptable> findByTenant_IdAndJournal_IdAndDateEcritureBetween(
            UUID tenantId, UUID journalComptableId, LocalDate startDate, LocalDate endDate
    );

    /* ============================================================================
       🧾 Requêtes personnalisées (optionnelles)
    ============================================================================ */

    @Query("""
           SELECT e FROM EcritureComptable e
           WHERE e.tenant.id = :tenantId
           AND e.dateEcriture BETWEEN :startDate AND :endDate
           AND e.validee = false
           """)
    List<EcritureComptable> findNonValidatedByDateRange(UUID tenantId, LocalDate startDate, LocalDate endDate);
}
