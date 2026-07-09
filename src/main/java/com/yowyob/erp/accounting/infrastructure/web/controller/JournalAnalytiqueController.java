package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.JournalAnalytiqueService;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAnalytiqueDto;
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
@RequestMapping("/api/accounting/analytique/journaux")
@RequiredArgsConstructor
@Tag(name = "Journaux Analytiques", description = "Gestion des journaux analytiques")
@SecurityRequirement(name = "BasicAuth")
public class JournalAnalytiqueController {

    private final JournalAnalytiqueService service;

    @PostMapping
    @Operation(summary = "Créer un journal analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<JournalAnalytiqueDto>>> create(@Valid @RequestBody JournalAnalytiqueDto dto) {
        return service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseWrapper.success(r, "Journal analytique créé")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un journal analytique par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<JournalAnalytiqueDto>>> findById(@PathVariable UUID id) {
        return service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Journal analytique trouvé")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un journal analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<JournalAnalytiqueDto>>> update(@PathVariable UUID id, @Valid @RequestBody JournalAnalytiqueDto dto) {
        return service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Journal analytique mis à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un journal analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return service.delete(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Journal analytique supprimé"))));
    }

    @GetMapping
    @Operation(summary = "Lister tous les journaux analytiques")
    public Mono<ResponseEntity<ApiResponseWrapper<List<JournalAnalytiqueDto>>>> getAll() {
        return service.getAll().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " journal/journaux analytique(s)")));
    }

    @GetMapping("/active")
    @Operation(summary = "Lister les journaux analytiques actifs")
    public Mono<ResponseEntity<ApiResponseWrapper<List<JournalAnalytiqueDto>>>> getActive() {
        return service.getActive().collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " journal/journaux analytique(s) actif(s)")));
    }
}
