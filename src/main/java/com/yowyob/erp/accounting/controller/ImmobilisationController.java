package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.AmortissementLigneDto;
import com.yowyob.erp.accounting.dto.ImmobilisationDto;
import com.yowyob.erp.accounting.service.ImmobilisationService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/immobilisations")
@RequiredArgsConstructor
@Tag(name = "Fixed Asset Management", description = "CRUD immobilisations, tableaux d'amortissement (Linéaire, Dégressif, Unités de Production), cession et mise au rebut")
@SecurityRequirement(name = "BasicAuth")
public class ImmobilisationController {

    private final ImmobilisationService immo_service;

    @PostMapping
    @Operation(summary = "Créer une immobilisation")
    public Mono<ResponseEntity<ApiResponseWrapper<ImmobilisationDto>>> create(
            @Valid @RequestBody ImmobilisationDto dto) {
        return immo_service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Immobilisation créée avec succès")));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les immobilisations de l'organisation")
    public Mono<ResponseEntity<ApiResponseWrapper<List<ImmobilisationDto>>>> findAll(
            @RequestParam(required = false) String statut) {
        var flux = statut != null ? immo_service.findByStatut(statut) : immo_service.findAll();
        return flux.collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list,
                list.size() + " immobilisation(s) trouvée(s)")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une immobilisation par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<ImmobilisationDto>>> findById(@PathVariable UUID id) {
        return immo_service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Immobilisation trouvée")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une immobilisation")
    public Mono<ResponseEntity<ApiResponseWrapper<ImmobilisationDto>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ImmobilisationDto dto) {
        return immo_service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Immobilisation mise à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une immobilisation (uniquement si aucun amortissement comptabilisé)")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return immo_service.delete(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Immobilisation supprimée"))));
    }

    @PostMapping("/{id}/cession")
    @Operation(summary = "Enregistrer la cession d'une immobilisation (génère l'écriture comptable)")
    public Mono<ResponseEntity<ApiResponseWrapper<ImmobilisationDto>>> ceder(
            @PathVariable UUID id,
            @RequestBody ImmobilisationDto dto) {
        return immo_service.ceder(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Cession enregistrée avec succès")));
    }

    @PostMapping("/{id}/rebut")
    @Operation(summary = "Mettre une immobilisation au rebut")
    public Mono<ResponseEntity<ApiResponseWrapper<ImmobilisationDto>>> rebut(@PathVariable UUID id) {
        return immo_service.mettreAuRebut(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Immobilisation mise au rebut")));
    }

    @GetMapping("/{id}/tableau-amortissement")
    @Operation(summary = "Obtenir le tableau d'amortissement d'une immobilisation")
    public Mono<ResponseEntity<ApiResponseWrapper<List<AmortissementLigneDto>>>> getTableau(@PathVariable UUID id) {
        return immo_service.getTableauAmortissement(id).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " ligne(s)")));
    }

    @PostMapping("/{id}/generate-schedule")
    @Operation(summary = "Générer/régénérer le tableau d'amortissement (Linéaire ou Dégressif)")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> generateSchedule(@PathVariable UUID id) {
        return immo_service.genererTableauAmortissement(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Tableau d'amortissement généré"))));
    }

    @PostMapping("/{id}/generate-schedule-unites-production")
    @Operation(summary = "Générer le tableau d'amortissement par unités de production")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> generateScheduleUP(
            @PathVariable UUID id,
            @RequestBody List<BigDecimal> unitesByYear) {
        return immo_service.genererTableauAmortissementUnitesProduction(id, unitesByYear)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Tableau UP généré"))));
    }

    @PostMapping("/post-depreciation")
    @Operation(summary = "Comptabiliser toutes les dotations aux amortissements d'un exercice")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> postDepreciation(@RequestParam UUID exerciceId) {
        return immo_service.comptabiliserAmortissements(exerciceId)
            .then(Mono.just(ResponseEntity.ok(
                ApiResponseWrapper.<Void>success(null, "Dotations aux amortissements comptabilisées"))));
    }
}
