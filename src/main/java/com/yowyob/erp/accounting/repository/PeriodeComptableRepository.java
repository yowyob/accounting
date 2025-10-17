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
public interface PeriodeComptableRepository extends JpaRepository<PeriodeComptable, UUID> {

  Optional<PeriodeComptable> findByTenant_IdAndId( UUID tenantId,UUID id);
  
    List<PeriodeComptable> findByTenant_IdOrderByDateDebutDesc(UUID tenantId);

    Optional<PeriodeComptable> findByTenant_IdAndCode(UUID tenantId, String code);

    @Query("""
           SELECT p FROM PeriodeComptable p 
           WHERE p.tenant.id = :tenantId 
           AND :date BETWEEN p.dateDebut AND p.dateFin
           """)
    Optional<PeriodeComptable> findByTenant_IdAndDateInRange(UUID tenantId, LocalDate date);

    List<PeriodeComptable> findByTenant_IdAndClotureeFalse(UUID tenantId);

    @Query("""
           SELECT p FROM PeriodeComptable p 
           WHERE p.tenant.id = :tenantId 
           AND p.dateDebut >= :startDate 
           AND p.dateFin <= :endDate
           """)
    List<PeriodeComptable> findByTenant_IdAndPeriodRange(UUID tenantId, LocalDate startDate, LocalDate endDate);
}
