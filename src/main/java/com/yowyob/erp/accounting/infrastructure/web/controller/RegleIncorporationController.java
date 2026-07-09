package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.RegleIncorporationService;
import com.yowyob.erp.accounting.infrastructure.web.dto.RegleIncorporationDto;
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
@RequestMapping("/api/accounting/analytique/regles-incorporation")
@RequiredArgsConstructor
@Tag(name = "Règles Incorporation", description = "Paramétrage des règles d'incorporation des charges CG")
@SecurityRequirement(name = "BasicAuth")
public class RegleIncorporationController {

    private final RegleIncorporationService service;

    @PostMapping
    @Operation(summary = "Créer une règle d'incorporation")
    public Mono<ResponseEntity<ApiResponseWrapper<RegleIncorporationDto>>> create(
            @Valid @RequestBody RegleIncorporationDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Règle d'incorporation créée")));
    }

    @GetMapping
    @Operation(summary = "Lister les règles d'incorporation")
    public Mono<ResponseEntity<ApiResponseWrapper<List<RegleIncorporationDto>>>> getAll() {
        return service.getAll().collectList()
            .map(list -> ResponseEntity.ok(
                ApiResponseWrapper.success(list, list.size() + " règle(s) d'incorporation")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une règle d'incorporation par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<RegleIncorporationDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Règle d'incorporation trouvée")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une règle d'incorporation")
    public Mono<ResponseEntity<ApiResponseWrapper<RegleIncorporationDto>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody RegleIncorporationDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Règle d'incorporation mise à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une règle d'incorporation")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(
                ApiResponseWrapper.<Void>success(null, "Règle d'incorporation supprimée"))));
    }
}
