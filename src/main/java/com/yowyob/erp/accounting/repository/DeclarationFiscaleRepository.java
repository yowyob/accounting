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
public interface DeclarationFiscaleRepository extends JpaRepository<DeclarationFiscale, UUID> {

    List<DeclarationFiscale> findByTenant_IdOrderByDateGenerationDesc(UUID tenantId);

    List<DeclarationFiscale> findByTenant_IdAndTypeDeclaration(UUID tenantId, String typeDeclaration);

    List<DeclarationFiscale> findByTenant_IdAndStatut(UUID tenantId, String statut);

    Optional<DeclarationFiscale> findByTenant_IdAndNumeroDeclaration(UUID tenantId, String numeroDeclaration);

    @Query("""
           SELECT d FROM DeclarationFiscale d 
           WHERE d.tenant.id = :tenantId 
           AND d.periodeDebut >= :startDate 
           AND d.periodeFin <= :endDate
           """)
    List<DeclarationFiscale> findByTenant_IdAndPeriodRange(UUID tenantId, LocalDate startDate, LocalDate endDate);

    boolean existsByTenant_IdAndNumeroDeclaration(UUID tenantId, String numeroDeclaration);
}
