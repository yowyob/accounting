package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.UniteOeuvreService;
import com.yowyob.erp.accounting.infrastructure.web.dto.UniteOeuvreDto;
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
@RequestMapping("/api/accounting/analytique/unites-oeuvre")
@RequiredArgsConstructor
@Tag(name = "Unités d'Œuvre", description = "Gestion des unités d'œuvre analytiques")
@SecurityRequirement(name = "BasicAuth")
public class UniteOeuvreController {

    private final UniteOeuvreService service;

    @PostMapping
    @Operation(summary = "Créer une unité d'œuvre")
    public Mono<ResponseEntity<ApiResponseWrapper<UniteOeuvreDto>>> create(@Valid @RequestBody UniteOeuvreDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseWrapper.success(r, "Unité d'œuvre créée")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une unité d'œuvre par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<UniteOeuvreDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Unité d'œuvre trouvée")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une unité d'œuvre")
    public Mono<ResponseEntity<ApiResponseWrapper<UniteOeuvreDto>>> update(@PathVariable UUID id, @Valid @RequestBody UniteOeuvreDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Unité d'œuvre mise à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une unité d'œuvre")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Unité d'œuvre supprimée"))));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les unités d'œuvre")
    public Mono<ResponseEntity<ApiResponseWrapper<List<UniteOeuvreDto>>>> getAll() {
        return service.getAll().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " unité(s) d'œuvre")));
    }

    @GetMapping("/active")
    @Operation(summary = "Lister les unités d'œuvre actives")
    public Mono<ResponseEntity<ApiResponseWrapper<List<UniteOeuvreDto>>>> getActive() {
        return service.getActive().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " unité(s) d'œuvre active(s)")));
    }

    @GetMapping("/by-centre/{centreId}")
    @Operation(summary = "Lister les unités d'œuvre d'un centre")
    public Mono<ResponseEntity<ApiResponseWrapper<List<UniteOeuvreDto>>>> getByCentre(@PathVariable UUID centreId) {
        return service.getByCentre(centreId).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " unité(s) d'œuvre pour ce centre")));
    }
}
