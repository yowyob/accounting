package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.EcritureAnalytiqueService;
import com.yowyob.erp.accounting.application.service.ImportCgService;
import com.yowyob.erp.accounting.infrastructure.web.dto.EcritureAnalytiqueDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.ImportCgRequestDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.ImportCgResultDto;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/analytique/ecritures")
@RequiredArgsConstructor
@Tag(name = "Écritures Analytiques", description = "Gestion des écritures et imputations analytiques")
@SecurityRequirement(name = "BasicAuth")
public class EcritureAnalytiqueController {

    private final EcritureAnalytiqueService service;
    private final ImportCgService importCgService;

    @PostMapping
    @Operation(summary = "Créer une écriture analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<EcritureAnalytiqueDto>>> create(
            @Valid @RequestBody EcritureAnalytiqueDto dto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return service.create(dto, idempotencyKey)
            .map(result -> {
                if (result.isAlreadyProcessed()) {
                    return ResponseEntity.ok(ApiResponseWrapper.success(
                        result.getDto(), "ALREADY_PROCESSED"));
                }
                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponseWrapper.success(result.getDto(), "Écriture analytique créée"));
            });
    }

    @GetMapping
    @Operation(summary = "Lister les écritures analytiques (filtrables par statut et période)")
    public Mono<ResponseEntity<ApiResponseWrapper<List<EcritureAnalytiqueDto>>>> getAll(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID periodeId) {
        return service.getAll(statut, periodeId).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " écriture(s)")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une écriture analytique par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<EcritureAnalytiqueDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Écriture trouvée")));
    }

    @PostMapping("/{id}/valider")
    @Operation(summary = "Valider une écriture analytique (BROUILLON → VALIDEE)")
    public Mono<ResponseEntity<ApiResponseWrapper<EcritureAnalytiqueDto>>> valider(@PathVariable UUID id) {
        return service.valider(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Écriture validée")));
    }

    @PostMapping("/import-cg")
    @Operation(summary = "Importer les charges CG incorporables en écritures analytiques brouillon")
    public Mono<ResponseEntity<ApiResponseWrapper<ImportCgResultDto>>> importCg(
            @RequestBody(required = false) ImportCgRequestDto request) {
        return importCgService.importFromCg(request)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponseWrapper.success(r, r.getCreated().size() + " écriture(s) importée(s)")));
    }

    @PostMapping("/{id}/rejeter")
    @Operation(summary = "Rejeter une écriture analytique (BROUILLON → REJETEE)")
    public Mono<ResponseEntity<ApiResponseWrapper<EcritureAnalytiqueDto>>> rejeter(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String raison = body.getOrDefault("raison", "Rejet sans motif");
        return service.rejeter(id, raison)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Écriture rejetée")));
    }
}
