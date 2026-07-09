package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.infrastructure.web.dto.CompteAnalytiqueDto;
import com.yowyob.erp.accounting.domain.port.in.CompteAnalytiqueUseCase;
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
@RequestMapping("/api/accounting/analytics/comptes")
@RequiredArgsConstructor
@Tag(name = "Analytics Accounts Management", description = "Gestion des comptes analytiques (natures de charges/produits)")
@SecurityRequirement(name = "BasicAuth")
public class CompteAnalytiqueController {

    private final CompteAnalytiqueUseCase compteService;

    @PostMapping
    @Operation(summary = "Créer un compte analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<CompteAnalytiqueDto>>> create(@Valid @RequestBody CompteAnalytiqueDto dto) {
        return compteService.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Compte analytique créé")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un compte analytique par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<CompteAnalytiqueDto>>> findById(@PathVariable UUID id) {
        return compteService.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Compte analytique trouvé")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un compte analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<CompteAnalytiqueDto>>> update(
            @PathVariable UUID id, @Valid @RequestBody CompteAnalytiqueDto dto) {
        return compteService.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Compte analytique mis à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un compte analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return compteService.delete(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Compte analytique supprimé"))));
    }

    @GetMapping
    @Operation(summary = "Lister tous les comptes analytiques de l'organisation")
    public Mono<ResponseEntity<ApiResponseWrapper<List<CompteAnalytiqueDto>>>> getAll() {
        return compteService.getAll().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " compte(s) analytique(s)")));
    }

    @GetMapping("/active")
    @Operation(summary = "Lister les comptes analytiques actifs de l'organisation")
    public Mono<ResponseEntity<ApiResponseWrapper<List<CompteAnalytiqueDto>>>> getActive() {
        return compteService.getActive().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " compte(s) analytique(s) actif(s)")));
    }
}
