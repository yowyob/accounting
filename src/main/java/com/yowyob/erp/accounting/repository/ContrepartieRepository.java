package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Contrepartie;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive R2DBC Repository for Contrepartie entity.
 */
@Repository
public interface ContrepartieRepository extends R2dbcRepository<Contrepartie, UUID> {

        @Query("SELECT * FROM contreparties WHERE organization_id = :organization_id AND operation_comptable_id = :operation_comptable_id")
        Flux<Contrepartie> findByTenant_IdAndOperation_comptable_Id(@Param("organization_id") UUID organization_id,
                        @Param("operation_comptable_id") UUID operation_comptable_id);

        @Query("SELECT * FROM contreparties WHERE organization_id = :organization_id AND compte_id = :compte_id")
        Flux<Contrepartie> findByTenant_IdAndCompte_id(@Param("organization_id") UUID organization_id, @Param("compte_id") UUID compte_id);

        @Query("DELETE FROM contreparties WHERE organization_id = :organization_id AND operation_comptable_id = :operation_comptable_id")
        Mono<Void> deleteByTenantIdAndOperationComptableId(@Param("organization_id") UUID organization_id,
                        @Param("operation_comptable_id") UUID operation_comptable_id);
}