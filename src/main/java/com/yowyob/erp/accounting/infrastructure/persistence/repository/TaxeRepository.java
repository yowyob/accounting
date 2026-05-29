package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.TaxeRepositoryPort;

import com.yowyob.erp.accounting.domain.model.Taxe;
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
public interface TaxeRepository extends R2dbcRepository<Taxe, UUID>, TaxeRepositoryPort {

    @Query("SELECT * FROM taxes WHERE organization_id = :organization_id")
    Flux<Taxe> findByOrganization_Id(@Param("organization_id") UUID organization_id);

    @Query("SELECT * FROM taxes WHERE organization_id = :organization_id AND actif = true")
    Flux<Taxe> findByOrganization_IdAndActifTrue(@Param("organization_id") UUID organization_id);

    @Query("SELECT * FROM taxes WHERE organization_id = :organization_id AND id = :id")
    Mono<Taxe> findByOrganization_IdAndId(@Param("organization_id") UUID organization_id, @Param("id") UUID id);

    @Query("SELECT * FROM taxes WHERE organization_id = :organization_id AND code = :code")
    Mono<Taxe> findByOrganization_IdAndCode(@Param("organization_id") UUID organization_id, @Param("code") String code);

    @Query("SELECT COUNT(*) > 0 FROM taxes WHERE organization_id = :organization_id AND code = :code")
    Mono<Boolean> existsByOrganization_IdAndCode(@Param("organization_id") UUID organization_id, @Param("code") String code);
}
