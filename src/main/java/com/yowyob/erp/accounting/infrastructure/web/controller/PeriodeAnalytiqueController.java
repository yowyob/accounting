package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.PeriodeAnalytiqueService;
import com.yowyob.erp.accounting.infrastructure.web.dto.PeriodeAnalytiqueDto;
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
@RequestMapping("/api/accounting/analytique/periodes")
@RequiredArgsConstructor
@Tag(name = "Périodes Analytiques", description = "Gestion des périodes analytiques")
@SecurityRequirement(name = "BasicAuth")
public class PeriodeAnalytiqueController {

    private final PeriodeAnalytiqueService service;

    @PostMapping
    @Operation(summary = "Créer une période analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<PeriodeAnalytiqueDto>>> create(@Valid @RequestBody PeriodeAnalytiqueDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseWrapper.success(r, "Période analytique créée")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une période analytique par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<PeriodeAnalytiqueDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Période analytique trouvée")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une période analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<PeriodeAnalytiqueDto>>> update(@PathVariable UUID id, @Valid @RequestBody PeriodeAnalytiqueDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Période analytique mise à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une période analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Période analytique supprimée"))));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les périodes analytiques")
    public Mono<ResponseEntity<ApiResponseWrapper<List<PeriodeAnalytiqueDto>>>> getAll() {
        return service.getAll().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " période(s) analytique(s)")));
    }

    @GetMapping("/statut/{statut}")
    @Operation(summary = "Lister les périodes analytiques par statut")
    public Mono<ResponseEntity<ApiResponseWrapper<List<PeriodeAnalytiqueDto>>>> getByStatut(@PathVariable String statut) {
        return service.getByStatut(statut).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " période(s) analytique(s) filtrée(s)")));
    }

    @GetMapping("/exercice/{exerciceId}")
    @Operation(summary = "Lister les périodes analytiques d'un exercice")
    public Mono<ResponseEntity<ApiResponseWrapper<List<PeriodeAnalytiqueDto>>>> getByExercice(@PathVariable UUID exerciceId) {
        return service.getByExercice(exerciceId).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " période(s) analytique(s) pour cet exercice")));
    }
}
