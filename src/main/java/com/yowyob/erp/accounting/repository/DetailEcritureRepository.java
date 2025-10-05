package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.DetailEcriture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DetailEcritureRepository extends JpaRepository<DetailEcriture, Long> {

    List<DetailEcriture> findByTenantId(UUID tenantId);

    List<DetailEcriture> findByTenantIdAndEcritureComptableId(UUID tenantId, Long ecritureId);

    List<DetailEcriture> findByTenantIdAndPlanComptableId(UUID tenantId, Long planComptableId);

    @Query("""
           SELECT d FROM DetailEcriture d 
           WHERE d.tenantId = :tenantId 
           AND d.dateEcriture BETWEEN :startDate AND :endDate
           """)
    List<DetailEcriture> findByTenantIdAndDateRange(UUID tenantId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("""
           SELECT COALESCE(SUM(d.montantDebit - d.montantCredit), 0) 
           FROM DetailEcriture d 
           WHERE d.tenantId = :tenantId AND d.planComptableId = :planComptableId
           """)
    Double calculateAccountBalance(UUID tenantId, Long planComptableId);
}
