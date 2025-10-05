package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.PeriodeComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PeriodeComptableRepository extends JpaRepository<PeriodeComptable, Long> {

    List<PeriodeComptable> findByTenantIdOrderByDateDebutDesc(UUID tenantId);

    Optional<PeriodeComptable> findByTenantIdAndCode(UUID tenantId, String code);

    @Query("""
           SELECT p FROM PeriodeComptable p 
           WHERE p.tenantId = :tenantId 
           AND :date BETWEEN p.dateDebut AND p.dateFin
           """)
    Optional<PeriodeComptable> findByTenantIdAndDateInRange(UUID tenantId, LocalDate date);

    List<PeriodeComptable> findByTenantIdAndClotureeFalse(UUID tenantId);

    @Query("""
           SELECT p FROM PeriodeComptable p 
           WHERE p.tenantId = :tenantId 
           AND p.dateDebut >= :startDate 
           AND p.dateFin <= :endDate
           """)
    List<PeriodeComptable> findByTenantIdAndPeriodRange(UUID tenantId, LocalDate startDate, LocalDate endDate);
}
