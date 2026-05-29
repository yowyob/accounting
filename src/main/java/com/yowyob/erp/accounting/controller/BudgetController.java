package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.BudgetDto;
import com.yowyob.erp.accounting.dto.BudgetVsRealiseDto;
import com.yowyob.erp.accounting.service.BudgetService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/budgets")
@RequiredArgsConstructor
@Tag(name = "Budget Management", description = "Gestion des budgets prévisionnels hiérarchiques et comparaison budget vs réalisé")
@SecurityRequirement(name = "BasicAuth")
public class BudgetController {

    private final BudgetService budget_service;

    @PostMapping
    @Operation(summary = "Créer un budget (hiérarchique ou analytique)")
    public Mono<ResponseEntity<ApiResponseWrapper<BudgetDto>>> create(@Valid @RequestBody BudgetDto dto) {
        return budget_service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Budget créé")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un budget par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<BudgetDto>>> findById(@PathVariable UUID id) {
        return budget_service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Budget trouvé")));
    }

    @GetMapping
    @Operation(summary = "Lister tous les budgets de l'organisation")
    public Mono<ResponseEntity<ApiResponseWrapper<List<BudgetDto>>>> getAll() {
        return budget_service.getAll().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " budget(s)")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un budget")
    public Mono<ResponseEntity<ApiResponseWrapper<BudgetDto>>> update(
            @PathVariable UUID id, @Valid @RequestBody BudgetDto dto) {
        return budget_service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Budget mis à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un budget")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return budget_service.delete(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Budget supprimé"))));
    }

    @GetMapping("/exercice/{exerciceId}")
    @Operation(summary = "Lister les budgets d'un exercice")
    public Mono<ResponseEntity<ApiResponseWrapper<List<BudgetDto>>>> findByExercice(@PathVariable UUID exerciceId) {
        return budget_service.findByExercice(exerciceId).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " budget(s)")));
    }

    @GetMapping("/periode/{periodeId}")
    @Operation(summary = "Lister les budgets d'une période")
    public Mono<ResponseEntity<ApiResponseWrapper<List<BudgetDto>>>> findByPeriode(@PathVariable UUID periodeId) {
        return budget_service.findByPeriode(periodeId).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " budget(s)")));
    }

    @GetMapping("/exercice/{exerciceId}/vs-realise")
    @Operation(summary = "Comparatif Budget vs Réalisé pour un exercice",
        description = "Retourne pour chaque compte le montant budgété, le réalisé, l'écart et le taux de réalisation")
    public Mono<ResponseEntity<ApiResponseWrapper<BudgetVsRealiseDto>>> getBudgetVsRealise(
            @PathVariable UUID exerciceId) {
        return budget_service.getBudgetVsRealise(exerciceId)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Comparatif budget vs réalisé")));
    }

    // ─────────────────────────────────────────────
    // WORKFLOW ENDPOINTS
    // ─────────────────────────────────────────────

    @PutMapping("/{id}/validate")
    @Operation(summary = "Valider un budget")
    public Mono<ResponseEntity<ApiResponseWrapper<BudgetDto>>> validate(@PathVariable UUID id) {
        return budget_service.validate(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Budget validé")));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activer un budget")
    public Mono<ResponseEntity<ApiResponseWrapper<BudgetDto>>> activate(@PathVariable UUID id) {
        return budget_service.activate(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Budget activé et en cours d'utilisation")));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Désactiver un budget")
    public Mono<ResponseEntity<ApiResponseWrapper<BudgetDto>>> deactivate(@PathVariable UUID id) {
        return budget_service.deactivate(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Budget désactivé")));
    }
}
