package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.CleRepartitionService;
import com.yowyob.erp.accounting.infrastructure.web.dto.CleRepartitionDto;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/analytique/repartitions")
@RequiredArgsConstructor
@Tag(name = "Clés de Répartition", description = "Gestion des clés de répartition analytiques")
@SecurityRequirement(name = "BasicAuth")
public class CleRepartitionController {

    private final CleRepartitionService service;

    @PostMapping
    @Operation(summary = "Créer une clé de répartition")
    public Mono<ResponseEntity<ApiResponseWrapper<CleRepartitionDto>>> create(@Valid @RequestBody CleRepartitionDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseWrapper.success(r, "Clé de répartition créée")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une clé de répartition par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<CleRepartitionDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Clé de répartition trouvée")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une clé de répartition")
    public Mono<ResponseEntity<ApiResponseWrapper<CleRepartitionDto>>> update(@PathVariable UUID id, @Valid @RequestBody CleRepartitionDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Clé de répartition mise à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une clé de répartition")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Clé de répartition supprimée"))));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les clés de répartition")
    public Mono<ResponseEntity<ApiResponseWrapper<List<CleRepartitionDto>>>> getAll() {
        return service.getAll().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " clé(s) de répartition")));
    }

    @GetMapping("/active")
    @Operation(summary = "Lister les clés de répartition actives")
    public Mono<ResponseEntity<ApiResponseWrapper<List<CleRepartitionDto>>>> getActive() {
        return service.getActive().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " clé(s) de répartition active(s)")));
    }
}
