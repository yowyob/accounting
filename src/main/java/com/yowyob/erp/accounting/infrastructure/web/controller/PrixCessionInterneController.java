package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.PrixCessionInterneService;
import com.yowyob.erp.accounting.infrastructure.web.dto.PrixCessionInterneDto;
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
@RequestMapping("/api/accounting/analytique/prix-cessions")
@RequiredArgsConstructor
@Tag(name = "Prix de Cessions Internes", description = "Tarification des échanges inter-centres")
@SecurityRequirement(name = "BasicAuth")
public class PrixCessionInterneController {

    private final PrixCessionInterneService service;

    @PostMapping
    @Operation(summary = "Créer un prix de cession interne")
    public Mono<ResponseEntity<ApiResponseWrapper<PrixCessionInterneDto>>> create(
            @Valid @RequestBody PrixCessionInterneDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Prix de cession créé")));
    }

    @GetMapping
    @Operation(summary = "Lister les prix de cession (filtres optionnels)")
    public Mono<ResponseEntity<ApiResponseWrapper<List<PrixCessionInterneDto>>>> getAll(
            @RequestParam(required = false) UUID centreCedantId,
            @RequestParam(required = false) UUID centreBeneficiaireId) {
        return service.getAll(centreCedantId, centreBeneficiaireId).collectList()
            .map(list -> ResponseEntity.ok(
                ApiResponseWrapper.success(list, list.size() + " prix de cession")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un prix de cession par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<PrixCessionInterneDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Prix de cession trouvé")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un prix de cession")
    public Mono<ResponseEntity<ApiResponseWrapper<PrixCessionInterneDto>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PrixCessionInterneDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Prix de cession mis à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un prix de cession")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(
                ApiResponseWrapper.<Void>success(null, "Prix de cession supprimé"))));
    }
}
