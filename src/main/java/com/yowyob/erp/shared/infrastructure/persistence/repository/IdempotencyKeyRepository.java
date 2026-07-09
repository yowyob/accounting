package com.yowyob.erp.shared.infrastructure.persistence.repository;

import com.yowyob.erp.shared.domain.model.IdempotencyKey;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends R2dbcRepository<IdempotencyKey, UUID> {

    @Query("""
        SELECT * FROM idempotency_keys
        WHERE organization_id = :orgId
          AND idempotency_key = :key
          AND expires_at > CURRENT_TIMESTAMP
        LIMIT 1
        """)
    Mono<IdempotencyKey> findActiveByOrganizationIdAndKey(
        @Param("orgId") UUID orgId,
        @Param("key") String key);
}
