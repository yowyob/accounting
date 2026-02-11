package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.TauxChange;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive R2DBC Repository for TauxChange entity.
 */
@Repository
public interface TauxChangeRepository extends R2dbcRepository<TauxChange, UUID> {

    @Query("SELECT * FROM taux_change WHERE organization_id = :organization_id")
    Flux<TauxChange> findByOrganization_Id(@Param("organization_id") UUID organization_id);

    /**
     * Finds the most recent exchange rate for a pair of currencies at or before a
     * specific date.
     */
    @Query("SELECT * FROM taux_change WHERE organization_id = :organization_id " +
            "AND devise_source_id = :source_id AND devise_cible_id = :target_id " +
            "AND date_effet <= :date ORDER BY date_effet DESC LIMIT 1")
    Mono<TauxChange> findMostRecentRate(@Param("organization_id") UUID organization_id,
            @Param("source_id") UUID source_id,
            @Param("target_id") UUID target_id,
            @Param("date") LocalDateTime date);
}
