package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.DeclarationFiscale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeclarationFiscaleRepository extends JpaRepository<DeclarationFiscale, Long> {

    List<DeclarationFiscale> findByTenantIdOrderByDateGenerationDesc(UUID tenantId);

    List<DeclarationFiscale> findByTenantIdAndTypeDeclaration(UUID tenantId, String typeDeclaration);

    List<DeclarationFiscale> findByTenantIdAndStatut(UUID tenantId, String statut);

    Optional<DeclarationFiscale> findByTenantIdAndNumeroDeclaration(UUID tenantId, String numeroDeclaration);

    @Query("""
           SELECT d FROM DeclarationFiscale d 
           WHERE d.tenantId = :tenantId 
           AND d.periodeDebut >= :startDate 
           AND d.periodeFin <= :endDate
           """)
    List<DeclarationFiscale> findByTenantIdAndPeriodRange(UUID tenantId, LocalDate startDate, LocalDate endDate);

    boolean existsByTenantIdAndNumeroDeclaration(UUID tenantId, String numeroDeclaration);
}
