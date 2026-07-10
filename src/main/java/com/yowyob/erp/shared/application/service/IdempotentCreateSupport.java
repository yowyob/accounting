package com.yowyob.erp.shared.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Support d'idempotence pour les CREATE offline (table {@code idempotency_keys}).
 * Ne nécessite pas de colonne {@code client_id} sur l'entité métier.
 */
@Component
@RequiredArgsConstructor
public class IdempotentCreateSupport {

    private final IdempotencyService idempotencyService;

    public record Result<T>(T data, boolean alreadyProcessed) {}

    public <T> Mono<Result<T>> create(
            UUID organizationId,
            String idempotencyKey,
            String entityType,
            Function<UUID, Mono<T>> loadById,
            Supplier<Mono<T>> persistNew,
            Function<T, UUID> extractId) {
        String key = blankToNull(idempotencyKey);
        if (key == null) {
            return persistNew.get().map(data -> new Result<>(data, false));
        }
        return idempotencyService.findActive(organizationId, key)
                .flatMap(record -> loadById.apply(record.getEntityId())
                        .map(existing -> new Result<>(existing, true)))
                .switchIfEmpty(Mono.defer(() -> persistNew.get()
                        .flatMap(created -> idempotencyService
                                .store(organizationId, key, entityType, extractId.apply(created), 201)
                                .thenReturn(new Result<>(created, false)))));
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
