package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.DeclarationFiscale;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Repository interface for managing tax declarations.
 * Refactored to Reactive R2DBC.
 * 
 * @author ALD
 * @date 30.12.25
 */
@Repository
public interface DeclarationFiscaleRepository extends R2dbcRepository<DeclarationFiscale, UUID> {

        /**
         * Finds all declarations for a tenant ordered by generation date descending.
         */
        @Query("SELECT * FROM declaration_fiscale WHERE organization_id = :organizationId ORDER BY date_generation DESC")
        Flux<DeclarationFiscale> findByTenantIdOrderByDateGenerationDesc(@Param("organizationId") UUID organizationId);

        /**
         * Finds a specific declaration for a tenant by its ID.
         */
        @Query("SELECT * FROM declaration_fiscale WHERE organization_id = :organizationId AND declaration_id = :id")
        Mono<DeclarationFiscale> findByTenantIdAndId(@Param("organizationId") UUID organizationId, @Param("id") UUID id);

        /**
         * Finds declarations for a tenant filtered by declaration type.
         */
        @Query("SELECT * FROM declaration_fiscale WHERE organization_id = :organizationId AND type_declaration = :typeDeclaration")
        Flux<DeclarationFiscale> findByTenantIdAndTypeDeclaration(@Param("organizationId") UUID organizationId,
                        @Param("typeDeclaration") String typeDeclaration);

        /**
         * Finds declarations for a tenant by their current status.
         */
        @Query("SELECT * FROM declaration_fiscale WHERE organization_id = :organizationId AND statut = :statut")
        Flux<DeclarationFiscale> findByTenantIdAndStatut(@Param("organizationId") UUID organizationId,
                        @Param("statut") String statut);

        /**
         * Finds a declaration by its unique business number for a specific tenant.
         */
        @Query("SELECT * FROM declaration_fiscale WHERE organization_id = :organizationId AND numero_declaration = :numeroDeclaration")
        Mono<DeclarationFiscale> findByTenantIdAndNumeroDeclaration(@Param("organizationId") UUID organizationId,
                        @Param("numeroDeclaration") String numeroDeclaration);

        /**
         * Finds declarations for a tenant within a specific date range based on period
         * start and end.
         */
        @Query("""
                        SELECT * FROM declaration_fiscale
                        WHERE organization_id = :organizationId
                        AND periode_debut >= :startDate
                        AND periode_fin <= :endDate
                        """)
        Flux<DeclarationFiscale> findByTenantIdAndPeriodRange(@Param("organizationId") UUID organizationId,
                        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Checks if a declaration number already exists for a given tenant.
         */
        @Query("SELECT COUNT(*) > 0 FROM declaration_fiscale WHERE organization_id = :organizationId AND numero_declaration = :numeroDeclaration")
        Mono<Boolean> existsByTenantIdAndNumeroDeclaration(@Param("organizationId") UUID organizationId,
                        @Param("numeroDeclaration") String numeroDeclaration);
}
