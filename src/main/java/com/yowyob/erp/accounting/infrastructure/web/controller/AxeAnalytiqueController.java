package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.infrastructure.web.dto.AxeAnalytiqueDto;
import com.yowyob.erp.accounting.domain.port.in.AxeAnalytiqueUseCase;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics Axis Management", description = "Gestion des axes analytiques (centres de coûts, projets, départements, etc.)")
@SecurityRequirement(name = "BasicAuth")
public class AxeAnalytiqueController {

    private final AxeAnalytiqueUseCase axeService;

    @PostMapping
    @Operation(summary = "Créer un axe analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<AxeAnalytiqueDto>>> create(@Valid @RequestBody AxeAnalytiqueDto dto) {
        return axeService.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Axe analytique créé")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un axe analytique par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<AxeAnalytiqueDto>>> findById(@PathVariable UUID id) {
        return axeService.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Axe analytique trouvé")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un axe analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<AxeAnalytiqueDto>>> update(
            @PathVariable UUID id, @Valid @RequestBody AxeAnalytiqueDto dto) {
        return axeService.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Axe analytique mis à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un axe analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return axeService.delete(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Axe analytique supprimé"))));
    }

    @GetMapping
    @Operation(summary = "Lister tous les axes analytiques de l'organisation")
    public Mono<ResponseEntity<ApiResponseWrapper<List<AxeAnalytiqueDto>>>> getAll() {
        return axeService.getAll().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " axe(s) analytique(s)")));
    }

    @GetMapping("/active")
    @Operation(summary = "Lister les axes analytiques actifs de l'organisation")
    public Mono<ResponseEntity<ApiResponseWrapper<List<AxeAnalytiqueDto>>>> getActive() {
        return axeService.getActive().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " axe(s) analytique(s) actif(s)")));
    }
}
