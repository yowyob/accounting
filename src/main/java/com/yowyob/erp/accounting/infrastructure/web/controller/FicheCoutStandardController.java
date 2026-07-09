package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.FicheCoutStandardService;
import com.yowyob.erp.accounting.infrastructure.web.dto.FicheCoutStandardDto;
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
@RequestMapping("/api/accounting/analytique/fiches-cout-standard")
@RequiredArgsConstructor
@Tag(name = "Fiches Coût Standard", description = "Fiches de coûts standards par produit")
@SecurityRequirement(name = "BasicAuth")
public class FicheCoutStandardController {

    private final FicheCoutStandardService service;

    @PostMapping
    @Operation(summary = "Créer une fiche de coût standard")
    public Mono<ResponseEntity<ApiResponseWrapper<FicheCoutStandardDto>>> create(
            @Valid @RequestBody FicheCoutStandardDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Fiche de coût standard créée")));
    }

    @GetMapping
    @Operation(summary = "Lister les fiches de coût standard (filtre période optionnel)")
    public Mono<ResponseEntity<ApiResponseWrapper<List<FicheCoutStandardDto>>>> getAll(
            @RequestParam(required = false) UUID periodeRefId) {
        return service.getAll(periodeRefId).collectList()
            .map(list -> ResponseEntity.ok(
                ApiResponseWrapper.success(list, list.size() + " fiche(s) de coût standard")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une fiche de coût standard par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<FicheCoutStandardDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Fiche de coût standard trouvée")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une fiche de coût standard")
    public Mono<ResponseEntity<ApiResponseWrapper<FicheCoutStandardDto>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody FicheCoutStandardDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Fiche de coût standard mise à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une fiche de coût standard")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(
                ApiResponseWrapper.<Void>success(null, "Fiche de coût standard supprimée"))));
    }
}
