package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.ConcordanceService;
import com.yowyob.erp.accounting.infrastructure.web.dto.ConcordanceCalculDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.ConcordancePeriodeDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.LigneConcordanceDto;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/analytique/concordance")
@RequiredArgsConstructor
@Tag(name = "Concordance CG/CA", description = "Rapprochement comptabilité générale / analytique")
@SecurityRequirement(name = "BasicAuth")
public class ConcordanceController {

    private final ConcordanceService service;

    @GetMapping("/periodes/{periodeId}")
    @Operation(summary = "Concordance complète pour une période (lignes manuelles + calcul)")
    public Mono<ResponseEntity<ApiResponseWrapper<ConcordancePeriodeDto>>> getPeriode(@PathVariable UUID periodeId) {
        return service.getPeriode(periodeId)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Concordance chargée")));
    }

    @GetMapping("/periodes/{periodeId}/calcul")
    @Operation(summary = "Calcul de concordance (lecture seule)")
    public Mono<ResponseEntity<ApiResponseWrapper<ConcordanceCalculDto>>> calcul(@PathVariable UUID periodeId) {
        return service.compute(periodeId)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Calcul de concordance")));
    }

    @GetMapping("/periodes/{periodeId}/lignes")
    @Operation(summary = "Lignes manuelles de concordance")
    public Mono<ResponseEntity<ApiResponseWrapper<List<LigneConcordanceDto>>>> getLignes(@PathVariable UUID periodeId) {
        return service.getLignesManuelles(periodeId)
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " ligne(s)")));
    }

    @PutMapping("/periodes/{periodeId}/lignes")
    @Operation(summary = "Remplacer les lignes manuelles de concordance")
    public Mono<ResponseEntity<ApiResponseWrapper<List<LigneConcordanceDto>>>> replaceLignes(
            @PathVariable UUID periodeId,
            @Valid @RequestBody List<LigneConcordanceDto> lignes) {
        return service.replaceLignesManuelles(periodeId, lignes)
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, "Lignes de concordance enregistrées")));
    }
}
