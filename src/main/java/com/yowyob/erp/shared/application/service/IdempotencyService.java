package com.yowyob.erp.shared.application.service;

import com.yowyob.erp.shared.domain.model.IdempotencyKey;
import com.yowyob.erp.shared.infrastructure.persistence.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final int TTL_HOURS = 72;

    private final IdempotencyKeyRepository repository;

    public Mono<IdempotencyKey> findActive(UUID organizationId, String key) {
        if (key == null || key.isBlank()) {
            return Mono.empty();
        }
        return repository.findActiveByOrganizationIdAndKey(organizationId, key);
    }

    public Mono<IdempotencyKey> store(
            UUID organizationId,
            String key,
            String entityType,
            UUID entityId,
            int httpStatus) {
        IdempotencyKey record = IdempotencyKey.builder()
            .id(UUID.randomUUID())
            .organizationId(organizationId)
            .idempotencyKey(key)
            .entityType(entityType)
            .entityId(entityId)
            .httpStatus(httpStatus)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(TTL_HOURS))
            .build();
        return repository.save(record);
    }
}
