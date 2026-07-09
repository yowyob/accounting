package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.EcritureAnalytique;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface EcritureAnalytiqueRepository extends R2dbcRepository<EcritureAnalytique, UUID> {
    Flux<EcritureAnalytique> findByOrganizationId(UUID organizationId);

    @Query("SELECT * FROM ecritures_analytiques WHERE organization_id = :orgId AND statut = :statut ORDER BY created_at DESC")
    Flux<EcritureAnalytique> findByOrganizationIdAndStatut(@Param("orgId") UUID orgId, @Param("statut") String statut);

    @Query("SELECT * FROM ecritures_analytiques WHERE organization_id = :orgId AND periode_id = :periodeId ORDER BY date_effet DESC")
    Flux<EcritureAnalytique> findByOrganizationIdAndPeriodeId(@Param("orgId") UUID orgId, @Param("periodeId") UUID periodeId);

    @Query("SELECT * FROM ecritures_analytiques WHERE organization_id = :orgId AND statut = :statut AND periode_id = :periodeId ORDER BY date_effet DESC")
    Flux<EcritureAnalytique> findByOrganizationIdAndStatutAndPeriodeId(@Param("orgId") UUID orgId, @Param("statut") String statut, @Param("periodeId") UUID periodeId);

    @Query("""
        SELECT COUNT(*) > 0 FROM ecritures_analytiques
        WHERE organization_id = :orgId
          AND origine = 'IMPORT_CG'
          AND ecriture_cg_ref = :ecritureCgRef
          AND montant_total = :montant
          AND libelle = :libelle
        """)
    Mono<Boolean> existsImportDuplicate(
        @Param("orgId") UUID orgId,
        @Param("ecritureCgRef") UUID ecritureCgRef,
        @Param("montant") java.math.BigDecimal montant,
        @Param("libelle") String libelle);

    @Query("""
        SELECT * FROM ecritures_analytiques
        WHERE organization_id = :orgId AND client_id = :clientId
        LIMIT 1
        """)
    Mono<EcritureAnalytique> findByOrganizationIdAndClientId(
        @Param("orgId") UUID orgId,
        @Param("clientId") String clientId);
}
