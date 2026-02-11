package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Agence;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository interface for managing Agence entities.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@Repository
public interface AgenceRepository extends R2dbcRepository<Agence, UUID> {

    @Query("SELECT * FROM agences WHERE organization_id = :organizationId")
    Flux<Agence> findByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("SELECT * FROM agences WHERE code = :code")
    Mono<Agence> findByCode(@Param("code") String code);

    @Query("SELECT * FROM agences WHERE organization_id = :organizationId AND code = :code")
    Mono<Agence> findByOrganizationIdAndCode(@Param("organizationId") UUID organizationId, @Param("code") String code);
}
