package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.DetailEcriture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DetailEcritureRepository extends JpaRepository<DetailEcriture, UUID> {

    List<DetailEcriture> findByTenant_Id(UUID tenantId);

    List<DetailEcriture> findByTenant_IdAndEcriture_Id(UUID tenantId, UUID ecritureId);

    List<DetailEcriture> findByTenant_IdAndCompte_Id(UUID tenantId, UUID compteId);

    @Query("""
           SELECT d FROM DetailEcriture d 
           WHERE d.tenant.id = :tenantId 
           AND d.dateEcriture BETWEEN :startDate AND :endDate
           """)
    List<DetailEcriture> findByTenant_IdAndDateRange(UUID tenantId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("""
           SELECT COALESCE(SUM(d.montantDebit - d.montantCredit), 0) 
           FROM DetailEcriture d 
           WHERE d.tenant.id = :tenantId AND d.compte.id = :planComptableId
           """)
    Double calculateAccountBalance(UUID tenantId, UUID planComptableId);
}
