package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Taxe;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive R2DBC Repository for Taxe entity.
 */
@Repository
public interface TaxeRepository extends R2dbcRepository<Taxe, UUID> {

    @Query("SELECT * FROM taxes WHERE organization_id = :organization_id")
    Flux<Taxe> findByTenant_Id(@Param("organization_id") UUID organization_id);

    @Query("SELECT * FROM taxes WHERE organization_id = :organization_id AND actif = true")
    Flux<Taxe> findByTenant_IdAndActifTrue(@Param("organization_id") UUID organization_id);

    @Query("SELECT * FROM taxes WHERE organization_id = :organization_id AND id = :id")
    Mono<Taxe> findByTenant_IdAndId(@Param("organization_id") UUID organization_id, @Param("id") UUID id);

    @Query("SELECT * FROM taxes WHERE organization_id = :organization_id AND code = :code")
    Mono<Taxe> findByTenant_IdAndCode(@Param("organization_id") UUID organization_id, @Param("code") String code);

    @Query("SELECT COUNT(*) > 0 FROM taxes WHERE organization_id = :organization_id AND code = :code")
    Mono<Boolean> existsByTenant_IdAndCode(@Param("organization_id") UUID organization_id, @Param("code") String code);
}
