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
         * Finds all declarations for a organization ordered by generation date descending.
         */
        @Query("SELECT * FROM declaration_fiscale WHERE organization_id = :organizationId ORDER BY date_generation DESC")
        Flux<DeclarationFiscale> findByOrganizationIdOrderByDateGenerationDesc(@Param("organizationId") UUID organizationId);

        /**
         * Finds a specific declaration for a organization by its ID.
         */
        @Query("SELECT * FROM declaration_fiscale WHERE organization_id = :organizationId AND declaration_id = :id")
        Mono<DeclarationFiscale> findByOrganizationIdAndId(@Param("organizationId") UUID organizationId, @Param("id") UUID id);

        /**
         * Finds declarations for a organization filtered by declaration type.
         */
        @Query("SELECT * FROM declaration_fiscale WHERE organization_id = :organizationId AND type_declaration = :typeDeclaration")
        Flux<DeclarationFiscale> findByOrganizationIdAndTypeDeclaration(@Param("organizationId") UUID organizationId,
                        @Param("typeDeclaration") String typeDeclaration);

        /**
         * Finds declarations for a organization by their current status.
         */
        @Query("SELECT * FROM declaration_fiscale WHERE organization_id = :organizationId AND statut = :statut")
        Flux<DeclarationFiscale> findByOrganizationIdAndStatut(@Param("organizationId") UUID organizationId,
                        @Param("statut") String statut);

        /**
         * Finds a declaration by its unique business number for a specific organization.
         */
        @Query("SELECT * FROM declaration_fiscale WHERE organization_id = :organizationId AND numero_declaration = :numeroDeclaration")
        Mono<DeclarationFiscale> findByOrganizationIdAndNumeroDeclaration(@Param("organizationId") UUID organizationId,
                        @Param("numeroDeclaration") String numeroDeclaration);

        /**
         * Finds declarations for a organization within a specific date range based on period
         * start and end.
         */
        @Query("""
                        SELECT * FROM declaration_fiscale
                        WHERE organization_id = :organizationId
                        AND periode_debut >= :startDate
                        AND periode_fin <= :endDate
                        """)
        Flux<DeclarationFiscale> findByOrganizationIdAndPeriodRange(@Param("organizationId") UUID organizationId,
                        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        /**
         * Checks if a declaration number already exists for a given organization.
         */
        @Query("SELECT COUNT(*) > 0 FROM declaration_fiscale WHERE organization_id = :organizationId AND numero_declaration = :numeroDeclaration")
        Mono<Boolean> existsByOrganizationIdAndNumeroDeclaration(@Param("organizationId") UUID organizationId,
                        @Param("numeroDeclaration") String numeroDeclaration);
}
