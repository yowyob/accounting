package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Compte;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * R2DBC Repository for OHADA accounting accounts management.
 * Implements reactive multi-tenant search operations.
 */
@Repository
public interface CompteRepository extends R2dbcRepository<Compte, UUID> {

        /** Finds an account by tenant and account number */
        @Query("SELECT * FROM comptes WHERE tenant_id = :tenant_id AND no_compte = :no_compte")
        Mono<Compte> findByTenant_IdAndNo_compte(@Param("tenant_id") UUID tenant_id,
                        @Param("no_compte") String no_compte);

        /** Finds an account by tenant and ID */
        @Query("SELECT * FROM comptes WHERE tenant_id = :tenant_id AND id = :id")
        Mono<Compte> findByTenant_IdAndId(@Param("tenant_id") UUID tenant_id, @Param("id") UUID id);

        /** Lists active accounts for a tenant */
        @Query("SELECT * FROM comptes WHERE tenant_id = :tenant_id AND actif = true")
        Flux<Compte> findByTenant_IdAndActifTrue(@Param("tenant_id") UUID tenant_id);

        /** Lists accounts for a tenant by OHADA class */
        @Query("SELECT * FROM comptes WHERE tenant_id = :tenant_id AND classe = :classe")
        Flux<Compte> findByTenant_IdAndClasse(@Param("tenant_id") UUID tenant_id, @Param("classe") Integer classe);

        /** Checks if an account exists for a tenant and a given number */
        @Query("SELECT COUNT(*) > 0 FROM comptes WHERE tenant_id = :tenant_id AND no_compte = :no_compte")
        Mono<Boolean> existsByTenant_IdAndNo_compte(@Param("tenant_id") UUID tenant_id,
                        @Param("no_compte") String no_compte);

        /** Finds accounts whose number starts with a given prefix */
        @Query("SELECT * FROM comptes WHERE tenant_id = :tenant_id AND no_compte LIKE CONCAT(:prefix, '%')")
        Flux<Compte> findByTenant_IdAndNo_compteStartingWith(@Param("tenant_id") UUID tenant_id,
                        @Param("prefix") String prefix);

        /** Finds the latest account number for a given prefix (max value) */
        @Query("SELECT * FROM comptes WHERE tenant_id = :tenant_id AND no_compte LIKE CONCAT(:prefix, '%') ORDER BY no_compte DESC LIMIT 1")
        Mono<Compte> findTopByTenant_IdAndNo_compteStartingWithOrderByNo_compteDesc(
                        @Param("tenant_id") UUID tenant_id,
                        @Param("prefix") String prefix);

        /** All accounts for a tenant (including inactive ones) */
        @Query("SELECT * FROM comptes WHERE tenant_id = :tenant_id")
        Flux<Compte> findAllByTenant_Id(@Param("tenant_id") UUID tenant_id);
}
