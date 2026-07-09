package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.LigneConcordance;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface LigneConcordanceRepository extends R2dbcRepository<LigneConcordance, UUID> {

    @Query("SELECT * FROM lignes_concordance WHERE organization_id = :orgId AND periode_id = :periodeId ORDER BY created_at ASC")
    Flux<LigneConcordance> findByOrganizationIdAndPeriodeId(
        @Param("orgId") UUID orgId,
        @Param("periodeId") UUID periodeId);

    @Query("DELETE FROM lignes_concordance WHERE organization_id = :orgId AND periode_id = :periodeId")
    Mono<Void> deleteByOrganizationIdAndPeriodeId(
        @Param("orgId") UUID orgId,
        @Param("periodeId") UUID periodeId);
}
