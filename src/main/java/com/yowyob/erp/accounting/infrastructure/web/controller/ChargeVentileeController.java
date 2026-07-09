package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.ChargeVentileeService;
import com.yowyob.erp.accounting.infrastructure.web.dto.ChargeVentileeDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.ChargeVentileeStatsDto;
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
@RequestMapping("/api/accounting/analytique/charges-ventilees")
@RequiredArgsConstructor
@Tag(name = "Charges Ventilées", description = "Ventilation des charges CG vers la comptabilité analytique")
@SecurityRequirement(name = "BasicAuth")
public class ChargeVentileeController {

    private final ChargeVentileeService service;

    @PostMapping
    @Operation(summary = "Créer une charge ventilée")
    public Mono<ResponseEntity<ApiResponseWrapper<ChargeVentileeDto>>> create(@Valid @RequestBody ChargeVentileeDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Charge ventilée créée")));
    }

    @GetMapping
    @Operation(summary = "Lister les charges ventilées (filtres optionnels)")
    public Mono<ResponseEntity<ApiResponseWrapper<List<ChargeVentileeDto>>>> getAll(
            @RequestParam(required = false) UUID periodeId,
            @RequestParam(required = false) Boolean incorporable) {
        return service.getAll(periodeId, incorporable).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " charge(s) ventilée(s)")));
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques de ventilation pour une période")
    public Mono<ResponseEntity<ApiResponseWrapper<ChargeVentileeStatsDto>>> stats(
            @RequestParam(required = false) UUID periodeId) {
        return service.getStats(periodeId)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Statistiques de ventilation")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une charge ventilée par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<ChargeVentileeDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Charge ventilée trouvée")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une charge ventilée")
    public Mono<ResponseEntity<ApiResponseWrapper<ChargeVentileeDto>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ChargeVentileeDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Charge ventilée mise à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une charge ventilée")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Charge ventilée supprimée"))));
    }
}
