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
 * Provides methods for auditing and filtering declarations by tenant and
 * period.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Repository
public interface DeclarationFiscaleRepository extends JpaRepository<DeclarationFiscale, UUID> {

        /**
         * Finds all declarations for a tenant ordered by generation date descending.
         * 
         * @param tenant_id the tenant ID
         * @return list of declarations
         */
        List<DeclarationFiscale> findByTenant_IdOrderByDate_generationDesc(@Param("tenant_id") UUID tenant_id);

        /**
         * Finds declarations for a tenant by ID.
         * 
         * @param tenant_id the tenant ID
         * @param id        the declaration ID
         * @return optional declaration
         */
        Optional<DeclarationFiscale> findByTenant_IdAndId(@Param("tenant_id") UUID tenant_id, @Param("id") UUID id);

        /**
         * Finds declarations for a tenant and type.
         * 
         * @param tenant_id        the tenant ID
         * @param type_declaration the declaration type
         * @return list of declarations
         */
        List<DeclarationFiscale> findByTenant_IdAndType_declaration(@Param("tenant_id") UUID tenant_id,
                        @Param("type_declaration") String type_declaration);

        /**
         * Finds declarations for a tenant and status.
         * 
         * @param tenant_id the tenant ID
         * @param statut    the declaration status
         * @return list of declarations
         */
        List<DeclarationFiscale> findByTenant_IdAndStatut(@Param("tenant_id") UUID tenant_id,
                        @Param("statut") String statut);

        /**
         * Finds a declaration by tenant and declaration number.
         * 
         * @param tenant_id          the tenant ID
         * @param numero_declaration the declaration number
         * @return optional declaration
         */
        Optional<DeclarationFiscale> findByTenant_IdAndNumero_declaration(@Param("tenant_id") UUID tenant_id,
                        @Param("numero_declaration") String numero_declaration);

        /**
         * Finds declarations for a tenant within a specific period range.
         * 
         * @param tenant_id  the tenant ID
         * @param start_date the start date
         * @param end_date   the end date
         * @return list of declarations
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
         * Checks if a declaration exists for a tenant and number.
         * 
         * @param tenant_id          the tenant ID
         * @param numero_declaration the declaration number
         * @return true if exists
         */
        boolean existsByTenant_IdAndNumero_declaration(@Param("tenant_id") UUID tenant_id,
                        @Param("numero_declaration") String numero_declaration);
}
