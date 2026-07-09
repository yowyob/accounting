package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.RegleValorisationStockService;
import com.yowyob.erp.accounting.infrastructure.web.dto.RegleValorisationStockDto;
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
@RequestMapping("/api/accounting/analytique/regles-valorisation-stock")
@RequiredArgsConstructor
@Tag(name = "Règles Valorisation Stock", description = "Paramétrage des méthodes de valorisation des stocks")
@SecurityRequirement(name = "BasicAuth")
public class RegleValorisationStockController {

    private final RegleValorisationStockService service;

    @PostMapping
    @Operation(summary = "Créer une règle de valorisation")
    public Mono<ResponseEntity<ApiResponseWrapper<RegleValorisationStockDto>>> create(
            @Valid @RequestBody RegleValorisationStockDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Règle de valorisation créée")));
    }

    @GetMapping
    @Operation(summary = "Lister les règles de valorisation")
    public Mono<ResponseEntity<ApiResponseWrapper<List<RegleValorisationStockDto>>>> getAll() {
        return service.getAll().collectList()
            .map(list -> ResponseEntity.ok(
                ApiResponseWrapper.success(list, list.size() + " règle(s) de valorisation")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une règle de valorisation par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<RegleValorisationStockDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Règle de valorisation trouvée")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une règle de valorisation")
    public Mono<ResponseEntity<ApiResponseWrapper<RegleValorisationStockDto>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody RegleValorisationStockDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Règle de valorisation mise à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une règle de valorisation")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(
                ApiResponseWrapper.<Void>success(null, "Règle de valorisation supprimée"))));
    }
}
