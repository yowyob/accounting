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
public interface EcritureComptableRepository extends JpaRepository<EcritureComptable, Long> {

    List<EcritureComptable> findByTenantId(UUID tenantId);

    Optional<EcritureComptable> findByTenantIdAndId(UUID tenantId, Long id);

    List<EcritureComptable> findByTenantIdAndValideeFalse(UUID tenantId);

    @Query("""
           SELECT e FROM EcritureComptable e 
           WHERE e.tenantId = :tenantId 
           AND e.dateEcriture BETWEEN :startDate AND :endDate
           """)
    List<EcritureComptable> findByTenantIdAndDateEcritureRange(UUID tenantId, LocalDate startDate, LocalDate endDate);

    @Query("""
           SELECT e FROM EcritureComptable e 
           WHERE e.tenantId = :tenantId 
           AND e.journalComptableId = :journalId 
           AND e.dateEcriture BETWEEN :startDate AND :endDate
           """)
    List<EcritureComptable> findByTenantIdAndJournalComptableIdAndDateRange(UUID tenantId, Long journalId, LocalDate startDate, LocalDate endDate);

    List<EcritureComptable> findByTenantIdAndJournalComptableId(UUID tenantId, Long journalId);
}
