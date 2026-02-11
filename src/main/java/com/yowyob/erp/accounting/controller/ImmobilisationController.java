package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.service.ImmobilisationService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST Controller for Fixed Asset Management.
 */
@RestController
@RequestMapping("/api/accounting/immobilisations")
@RequiredArgsConstructor
@Tag(name = "Fixed Asset Management", description = "Endpoints for asset tracking and depreciation")
public class ImmobilisationController {

    private final ImmobilisationService immo_service;

    @PostMapping("/{id}/generate-schedule")
    @Operation(summary = "Generate depreciation schedule for an asset")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> generateSchedule(@PathVariable UUID id) {
        return immo_service.genererTableauAmortissement(id)
                .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.success(null, "Tableau d'amortissement généré"))));
    }

    @PostMapping("/post-depreciation")
    @Operation(summary = "Post all pending depreciation entries for a fiscal year")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> postDepreciation(@RequestParam UUID exerciceId) {
        return immo_service.comptabiliserAmortissements(exerciceId)
                .then(Mono.just(ResponseEntity
                        .ok(ApiResponseWrapper.success(null, "Dotations aux amortissements comptabilisées"))));
    }
}
