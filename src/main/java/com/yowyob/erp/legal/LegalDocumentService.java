package com.yowyob.erp.legal;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Récupère les documents légaux depuis le kernel et les met en cache localement.
 *
 * <p>Flux : frontend → backend (ce service) → kernel. Le résultat est mis en cache mémoire avec un
 * TTL ; tant que le cache est valide, aucune requête n'est émise vers le kernel. En cas
 * d'indisponibilité du kernel, on sert la dernière version connue (cache périmé) si elle existe :
 * cohérent avec la philosophie online-first + repli du projet.</p>
 */
@Service
public class LegalDocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegalDocumentService.class);
    private static final String LIST_CACHE_KEY = "__list__";

    private record CacheEntry<T>(T value, Instant expiresAt) {
        boolean isFresh() {
            return expiresAt.isAfter(Instant.now());
        }
    }

    private final WebClient kernelWebClient;
    private final String kernelPath;
    private final long ttlSeconds;

    private final Map<String, CacheEntry<LegalDocumentDto>> documentCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<List<LegalDocumentDto>>> listCache = new ConcurrentHashMap<>();

    public LegalDocumentService(WebClient kernelWebClient,
                                @Value("${legal.kernel.path:/api/settings/legal-documents}") String kernelPath,
                                @Value("${legal.cache.ttl-seconds:600}") long ttlSeconds) {
        this.kernelWebClient = kernelWebClient;
        this.kernelPath = kernelPath;
        this.ttlSeconds = ttlSeconds;
    }

    public Mono<LegalDocumentDto> get(String slug) {
        String key = slug == null ? "" : slug.trim().toLowerCase();
        CacheEntry<LegalDocumentDto> cached = documentCache.get(key);
        if (cached != null && cached.isFresh()) {
            return Mono.just(cached.value());
        }
        return kernelWebClient.get()
                .uri(kernelPath + "/" + key)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(root -> toDto(root.path("data")))
                .doOnNext(dto -> documentCache.put(key, new CacheEntry<>(dto, Instant.now().plusSeconds(ttlSeconds))))
                .onErrorResume(error -> fallback(key, cached, error, slug));
    }

    public Mono<List<LegalDocumentDto>> list() {
        CacheEntry<List<LegalDocumentDto>> cached = listCache.get(LIST_CACHE_KEY);
        if (cached != null && cached.isFresh()) {
            return Mono.just(cached.value());
        }
        return kernelWebClient.get()
                .uri(kernelPath)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(root -> {
                    JsonNode data = root.path("data");
                    java.util.ArrayList<LegalDocumentDto> documents = new java.util.ArrayList<>();
                    if (data.isArray()) {
                        data.forEach(node -> documents.add(toDto(node)));
                    }
                    return (List<LegalDocumentDto>) documents;
                })
                .doOnNext(docs -> listCache.put(LIST_CACHE_KEY, new CacheEntry<>(docs, Instant.now().plusSeconds(ttlSeconds))))
                .onErrorResume(error -> {
                    if (cached != null) {
                        LOGGER.warn("Kernel injoignable pour la liste des documents légaux, service du cache périmé: {}",
                                error.getMessage());
                        return Mono.just(cached.value());
                    }
                    return Mono.error(error);
                });
    }

    private Mono<LegalDocumentDto> fallback(String key, CacheEntry<LegalDocumentDto> cached, Throwable error, String slug) {
        if (error instanceof WebClientResponseException.NotFound) {
            return Mono.error(new LegalDocumentNotFoundException(slug));
        }
        if (cached != null) {
            LOGGER.warn("Kernel injoignable pour le document légal '{}', service du cache périmé: {}", key,
                    error.getMessage());
            return Mono.just(cached.value());
        }
        return Mono.error(error);
    }

    private LegalDocumentDto toDto(JsonNode node) {
        return new LegalDocumentDto(
                text(node, "slug"),
                text(node, "locale"),
                text(node, "title"),
                text(node, "version"),
                text(node, "content"),
                text(node, "updatedAt"));
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
