package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.ChargeAnalytiqueService;
import com.yowyob.erp.accounting.infrastructure.web.dto.ChargeAnalytiqueDto;
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
@RequestMapping("/api/accounting/analytique/charges")
@RequiredArgsConstructor
@Tag(name = "Charges Analytiques", description = "Saisie des charges directes et indirectes")
@SecurityRequirement(name = "BasicAuth")
public class ChargeAnalytiqueController {

    private final ChargeAnalytiqueService service;

    @PostMapping
    @Operation(summary = "Créer une charge analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<ChargeAnalytiqueDto>>> create(
            @Valid @RequestBody ChargeAnalytiqueDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Charge analytique créée")));
    }

    @GetMapping
    @Operation(summary = "Lister les charges analytiques (filtres optionnels)")
    public Mono<ResponseEntity<ApiResponseWrapper<List<ChargeAnalytiqueDto>>>> getAll(
            @RequestParam(required = false) UUID periodeId,
            @RequestParam(required = false) String type) {
        return service.getAll(periodeId, type).collectList()
            .map(list -> ResponseEntity.ok(
                ApiResponseWrapper.success(list, list.size() + " charge(s) analytique(s)")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une charge analytique par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<ChargeAnalytiqueDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Charge analytique trouvée")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une charge analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<ChargeAnalytiqueDto>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ChargeAnalytiqueDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Charge analytique mise à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une charge analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(
                ApiResponseWrapper.<Void>success(null, "Charge analytique supprimée"))));
    }
}
