package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.DeclarationFiscale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing tax declarations.
 * Provides methods for auditing and filtering declarations by tenant and period.
 * * Note: Uses explicit JPQL queries to handle snake_case naming conventions
 * in the entity fields and avoid Spring Data method name parsing conflicts.
 * * @author ALD
 * @date 30.12.25
 */
@Repository
public interface DeclarationFiscaleRepository extends JpaRepository<DeclarationFiscale, UUID> {

    /**
     * Finds all declarations for a tenant ordered by generation date descending.
     * * @param tenant_id the tenant ID
     * @return list of declarations
     */
    @Query("SELECT d FROM DeclarationFiscale d WHERE d.tenant.id = :tenant_id ORDER BY d.date_generation DESC")
    List<DeclarationFiscale> findByTenant_IdOrderByDate_generationDesc(@Param("tenant_id") UUID tenant_id);

    /**
     * Finds a specific declaration for a tenant by its ID.
     * * @param tenant_id the tenant ID
     * @param id the declaration ID
     * @return optional declaration
     */
    @Query("SELECT d FROM DeclarationFiscale d WHERE d.tenant.id = :tenant_id AND d.id = :id")
    Optional<DeclarationFiscale> findByTenant_IdAndId(@Param("tenant_id") UUID tenant_id, @Param("id") UUID id);

    /**
     * Finds declarations for a tenant filtered by declaration type (VAT, IS, etc.).
     * * @param tenant_id the tenant ID
     * @param type_declaration the type of tax declaration
     * @return list of matching declarations
     */
    @Query("SELECT d FROM DeclarationFiscale d WHERE d.tenant.id = :tenant_id AND d.type_declaration = :type_declaration")
    List<DeclarationFiscale> findByTenant_IdAndType_declaration(@Param("tenant_id") UUID tenant_id,
                                @Param("type_declaration") String type_declaration);

    /**
     * Finds declarations for a tenant by their current status (DRAFT, SUBMITTED, VALIDATED).
     * * @param tenant_id the tenant ID
     * @param statut the status to filter by
     * @return list of declarations
     */
    @Query("SELECT d FROM DeclarationFiscale d WHERE d.tenant.id = :tenant_id AND d.statut = :statut")
    List<DeclarationFiscale> findByTenant_IdAndStatut(@Param("tenant_id") UUID tenant_id,
                                @Param("statut") String statut);

    /**
     * Finds a declaration by its unique business number for a specific tenant.
     * * @param tenant_id the tenant ID
     * @param numero_declaration the unique declaration number
     * @return optional declaration
     */
    @Query("SELECT d FROM DeclarationFiscale d WHERE d.tenant.id = :tenant_id AND d.numero_declaration = :numero_declaration")
    Optional<DeclarationFiscale> findByTenant_IdAndNumero_declaration(@Param("tenant_id") UUID tenant_id,
                                @Param("numero_declaration") String numero_declaration);

    /**
     * Finds declarations for a tenant within a specific date range based on period start and end.
     * * @param tenant_id the tenant ID
     * @param start_date the inclusive start date of the period
     * @param end_date the inclusive end date of the period
     * @return list of declarations found within the range
     */
    @Query("""
            SELECT d FROM DeclarationFiscale d
            WHERE d.tenant.id = :tenant_id
            AND d.periode_debut >= :start_date
            AND d.periode_fin <= :end_date
            """)
    List<DeclarationFiscale> findByTenant_IdAndPeriodRange(@Param("tenant_id") UUID tenant_id,
                                @Param("start_date") LocalDate start_date, @Param("end_date") LocalDate end_date);

    /**
     * Checks if a declaration number already exists for a given tenant.
     * * @param tenant_id the tenant ID
     * @param numero_declaration the declaration number to check
     * @return true if a record exists, false otherwise
     */
    @Query("SELECT COUNT(d) > 0 FROM DeclarationFiscale d WHERE d.tenant.id = :tenant_id AND d.numero_declaration = :numero_declaration")
    boolean existsByTenant_IdAndNumero_declaration(@Param("tenant_id") UUID tenant_id,
                                @Param("numero_declaration") String numero_declaration);
}
