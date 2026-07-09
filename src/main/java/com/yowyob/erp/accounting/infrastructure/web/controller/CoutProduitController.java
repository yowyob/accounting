package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.CoutProduitService;
import com.yowyob.erp.accounting.infrastructure.web.dto.CoutProduitDto;
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
@RequestMapping("/api/accounting/analytique/couts-produits")
@RequiredArgsConstructor
@Tag(name = "Coûts Produits", description = "Coûts complets par produit et période")
@SecurityRequirement(name = "BasicAuth")
public class CoutProduitController {

    private final CoutProduitService service;

    @PostMapping
    @Operation(summary = "Créer un coût produit")
    public Mono<ResponseEntity<ApiResponseWrapper<CoutProduitDto>>> create(
            @Valid @RequestBody CoutProduitDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Coût produit créé")));
    }

    @GetMapping
    @Operation(summary = "Lister les coûts produits (filtre période optionnel)")
    public Mono<ResponseEntity<ApiResponseWrapper<List<CoutProduitDto>>>> getAll(
            @RequestParam(required = false) UUID periodeId) {
        return service.getAll(periodeId).collectList()
            .map(list -> ResponseEntity.ok(
                ApiResponseWrapper.success(list, list.size() + " coût(s) produit")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un coût produit par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<CoutProduitDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Coût produit trouvé")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un coût produit")
    public Mono<ResponseEntity<ApiResponseWrapper<CoutProduitDto>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CoutProduitDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Coût produit mis à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un coût produit")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(
                ApiResponseWrapper.<Void>success(null, "Coût produit supprimé"))));
    }
}
