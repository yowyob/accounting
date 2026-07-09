package com.yowyob.erp.accounting.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.OrganizationRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.OrganizationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Remplace les noms techniques provisionnés côté accounting (ex. « Organization &lt;uuid&gt; »)
 * par le libellé métier du Kernel lorsque disponible.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationNameEnricher {

    private static final Pattern PLACEHOLDER_NAME = Pattern.compile(
            "^Organization\\s+[0-9a-f]{8}", Pattern.CASE_INSENSITIVE);

    private static final List<String> KERNEL_NAME_FIELDS = List.of(
            "displayName", "shortName", "longName", "legalName", "code");

    private final WebClient kernelWebClient;
    private final OrganizationRepository organizationRepository;

    public static boolean isPlaceholderName(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        String trimmed = name.trim();
        if ("Organization".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if (PLACEHOLDER_NAME.matcher(trimmed).find()) {
            return true;
        }
        try {
            UUID.fromString(trimmed);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public Mono<OrganizationDto> enrichIfNeeded(
            OrganizationDto dto,
            String authorization,
            String tenantId,
            String organizationId) {
        if (dto == null) {
            return Mono.empty();
        }
        if (!isPlaceholderName(dto.getName())) {
            return Mono.just(dto);
        }
        if (authorization == null || authorization.isBlank()) {
            return Mono.just(dto);
        }

        return kernelWebClient.get()
                .uri("/api/organizations/{id}", dto.getId())
                .headers(headers -> {
                    headers.set(HttpHeaders.AUTHORIZATION, authorization);
                    if (tenantId != null && !tenantId.isBlank()) {
                        headers.set("X-Tenant-Id", tenantId);
                    }
                    if (organizationId != null && !organizationId.isBlank()) {
                        headers.set("X-Organization-Id", organizationId);
                    }
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::extractKernelName)
                .filter(name -> !name.isBlank())
                .flatMap(name -> organizationRepository.findById(dto.getId())
                        .flatMap(org -> persistName(org, name)))
                .defaultIfEmpty(dto)
                .onErrorResume(error -> {
                    log.warn("Kernel organization name enrichment failed for {}: {}",
                            dto.getId(), error.getMessage());
                    return Mono.just(dto);
                });
    }

    private Mono<OrganizationDto> persistName(Organization org, String name) {
        org.setName(name);
        org.setUpdated_at(LocalDateTime.now());
        org.setNotNew();
        return organizationRepository.save(org)
                .map(saved -> OrganizationDto.builder()
                        .id(saved.getId())
                        .name(saved.getName())
                        .description(saved.getDescription())
                        .address(saved.getAddress())
                        .tax_id(saved.getTax_id())
                        .created_at(saved.getCreated_at())
                        .updated_at(saved.getUpdated_at())
                        .build());
    }

    private String extractKernelName(JsonNode json) {
        JsonNode data = json.has("data") ? json.get("data") : json;
        for (String field : KERNEL_NAME_FIELDS) {
            String value = data.path(field).asText("");
            if (!value.isBlank() && !isPlaceholderName(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
