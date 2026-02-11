package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.ReleveBancaireDto;
import com.yowyob.erp.accounting.entity.ReleveBancaire;
import com.yowyob.erp.accounting.service.RapprochementBancaireService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Bank Reconciliation (Rapprochement Bancaire).
 */
@RestController
@RequestMapping("/api/accounting/bank-statements")
@RequiredArgsConstructor
@Tag(name = "Bank Reconciliation", description = "Endpoints for importing statements and reconciling entries")
public class RapprochementBancaireController {

    private final RapprochementBancaireService rapprochement_service;

    @PostMapping("/import")
    @Operation(summary = "Import bank statement lines")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> importReleve(@RequestBody List<ReleveBancaireDto> lignesDto) {
        List<ReleveBancaire> lignes = lignesDto.stream()
                .map(this::mapToEntity)
                .collect(Collectors.toList());
        return rapprochement_service.importerReleve(lignes)
                .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.success(null, "Relevé importé avec succès"))));
    }

    @GetMapping("/{id}/candidates")
    @Operation(summary = "Get potential reconciliation candidates for a bank line")
    public Mono<ResponseEntity<ApiResponseWrapper<Object>>> getCandidates(@PathVariable UUID id) {
        return rapprochement_service.proposerRapprochement(id)
                .collectList()
                .map(candidates -> ResponseEntity.ok(ApiResponseWrapper.success(candidates, "Candidats trouvés")));
    }

    @PostMapping("/{releveId}/reconcile/{detailId}")
    @Operation(summary = "Formally reconcile a bank line with a ledger entry")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> reconcile(
            @PathVariable UUID releveId,
            @PathVariable UUID detailId) {
        return rapprochement_service.validerRapprochement(releveId, detailId)
                .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.success(null, "Rapprochement effectué"))));
    }

    private ReleveBancaire mapToEntity(ReleveBancaireDto dto) {
        return ReleveBancaire.builder()
                .id(dto.getId())
                .organizationId(dto.getOrganizationId())
                .compteId(dto.getCompteId())
                .dateOperation(dto.getDateOperation())
                .dateValeur(dto.getDateValeur())
                .libelle(dto.getLibelle())
                .reference(dto.getReference())
                .montant(dto.getMontant())
                .sens(dto.getSens())
                .categorieDetectee(dto.getCategorieDetectee())
                .rapproche(dto.isRapproche())
                .dateRapprochement(dto.getDateRapprochement())
                .detailEcritureId(dto.getDetailEcritureId())
                .isNew(dto.getId() == null)
                .build();
    }
}
