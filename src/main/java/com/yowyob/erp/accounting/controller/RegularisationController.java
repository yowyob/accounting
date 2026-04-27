package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.RegularisationDto;
import com.yowyob.erp.accounting.entity.StatutRegularisation;
import com.yowyob.erp.accounting.entity.TypeRegularisation;
import com.yowyob.erp.accounting.service.RegularisationService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
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
@RequestMapping("/api/accounting/regularisations")
@RequiredArgsConstructor
@Tag(name = "Régularisations OHADA", description = "Gestion des régularisations de fin de période : CCA (476), PCA (477), CAP (408/428/448), PAR (418/438)")
@SecurityRequirement(name = "BasicAuth")
public class RegularisationController {

    private final RegularisationService regularisation_service;

    @PostMapping
    @Operation(summary = "Créer une régularisation",
        description = "Génère l'écriture initiale et planifie l'extourne automatique au 1er jour de la période suivante")
    public Mono<ResponseEntity<ApiResponseWrapper<RegularisationDto>>> create(
            @Valid @RequestBody RegularisationDto dto) {
        return regularisation_service.createRegularisation(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Régularisation créée")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une régularisation par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<RegularisationDto>>> findById(@PathVariable UUID id) {
        return regularisation_service.getById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Régularisation trouvée")));
    }

    @GetMapping
    @Operation(summary = "Lister toutes les régularisations de l'organisation")
    public Mono<ResponseEntity<ApiResponseWrapper<List<RegularisationDto>>>> findAll() {
        return regularisation_service.getAll().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " régularisation(s)")));
    }

    @GetMapping("/periode/{periodeId}")
    @Operation(summary = "Lister les régularisations d'une période")
    public Mono<ResponseEntity<ApiResponseWrapper<List<RegularisationDto>>>> findByPeriode(
            @PathVariable UUID periodeId) {
        return regularisation_service.getByPeriode(periodeId).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " régularisation(s)")));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Lister les régularisations par type (CCA, PCA, CAP, PAR)")
    public Mono<ResponseEntity<ApiResponseWrapper<List<RegularisationDto>>>> findByType(
            @PathVariable TypeRegularisation type) {
        return regularisation_service.getByType(type).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " régularisation(s)")));
    }

    @GetMapping("/statut/{statut}")
    @Operation(summary = "Lister les régularisations par statut (ACTIVE, EXTOURNEE, ANNULEE)")
    public Mono<ResponseEntity<ApiResponseWrapper<List<RegularisationDto>>>> findByStatut(
            @PathVariable StatutRegularisation statut) {
        return regularisation_service.getByStatut(statut).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " régularisation(s)")));
    }

    @PostMapping("/{id}/extourner")
    @Operation(summary = "Extourner manuellement une régularisation",
        description = "Génère l'écriture d'extourne (inverse) et marque la régularisation comme EXTOURNEE")
    public Mono<ResponseEntity<ApiResponseWrapper<RegularisationDto>>> extourner(@PathVariable UUID id) {
        return regularisation_service.extourner(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Régularisation extournée")));
    }

    @PostMapping("/extourner-dues")
    @Operation(summary = "Extourner toutes les régularisations dont la date d'extourne est dépassée",
        description = "Utilisé à l'ouverture d'une nouvelle période pour extourner automatiquement les CCA/PCA/CAP/PAR échus")
    public Mono<ResponseEntity<ApiResponseWrapper<Long>>> extournerDues() {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(tuple -> regularisation_service.extournerDues(tuple.getT1(), tuple.getT2()))
            .map(count -> ResponseEntity.ok(ApiResponseWrapper.success(count, count + " régularisation(s) extournée(s)")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Annuler une régularisation active")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> annuler(@PathVariable UUID id) {
        return regularisation_service.annuler(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Régularisation annulée"))));
    }
}
