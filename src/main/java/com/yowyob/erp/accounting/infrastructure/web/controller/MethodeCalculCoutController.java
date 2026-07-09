package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.MethodeCalculCoutService;
import com.yowyob.erp.accounting.infrastructure.web.dto.MethodeCalculCoutDto;
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
@RequestMapping("/api/accounting/analytique/methodes-calcul-cout")
@RequiredArgsConstructor
@Tag(name = "Méthodes Calcul Coût", description = "Paramétrage des méthodes de calcul des coûts")
@SecurityRequirement(name = "BasicAuth")
public class MethodeCalculCoutController {

    private final MethodeCalculCoutService service;

    @PostMapping
    @Operation(summary = "Créer une méthode de calcul")
    public Mono<ResponseEntity<ApiResponseWrapper<MethodeCalculCoutDto>>> create(
            @Valid @RequestBody MethodeCalculCoutDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Méthode de calcul créée")));
    }

    @GetMapping
    @Operation(summary = "Lister les méthodes de calcul (filtres optionnels)")
    public Mono<ResponseEntity<ApiResponseWrapper<List<MethodeCalculCoutDto>>>> getAll(
            @RequestParam(required = false) String planAnalytiqueId,
            @RequestParam(required = false) String statut) {
        return service.getAll(planAnalytiqueId, statut).collectList()
            .map(list -> ResponseEntity.ok(
                ApiResponseWrapper.success(list, list.size() + " méthode(s) de calcul")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une méthode de calcul par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<MethodeCalculCoutDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Méthode de calcul trouvée")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une méthode de calcul")
    public Mono<ResponseEntity<ApiResponseWrapper<MethodeCalculCoutDto>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody MethodeCalculCoutDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Méthode de calcul mise à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une méthode de calcul archivée")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(
                ApiResponseWrapper.<Void>success(null, "Méthode de calcul supprimée"))));
    }
}
