package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.infrastructure.web.dto.LignePaieDto;
import com.yowyob.erp.accounting.domain.port.in.PaieUseCase;
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
@RequestMapping("/api/accounting/paie")
@RequiredArgsConstructor
@Tag(name = "Payroll (Paie)", description = "Gestion des bulletins de salaires et comptabilisation OHADA (6611, 4311, 4441, 4220, 6613, 4312)")
@SecurityRequirement(name = "BasicAuth")
public class PaieController {

    private final PaieUseCase paie_service;

    @PostMapping
    @Operation(summary = "Saisir un bulletin de paie mensuel agrégé")
    public Mono<ResponseEntity<ApiResponseWrapper<LignePaieDto>>> create(@Valid @RequestBody LignePaieDto dto) {
        return paie_service.create(dto)
            .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(r, "Bulletin de paie créé")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un bulletin de paie par ID")
    public Mono<ResponseEntity<ApiResponseWrapper<LignePaieDto>>> findById(@PathVariable UUID id) {
        return paie_service.findById(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Bulletin trouvé")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un bulletin de paie (interdit si comptabilisé)")
    public Mono<ResponseEntity<ApiResponseWrapper<LignePaieDto>>> update(
            @PathVariable UUID id, @Valid @RequestBody LignePaieDto dto) {
        return paie_service.update(id, dto)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Bulletin mis à jour")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un bulletin de paie (interdit si comptabilisé)")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> delete(@PathVariable UUID id) {
        return paie_service.delete(id)
            .then(Mono.just(ResponseEntity.ok(ApiResponseWrapper.<Void>success(null, "Bulletin supprimé"))));
    }

    @GetMapping("/exercice/{exerciceId}")
    @Operation(summary = "Lister les bulletins de paie d'un exercice")
    public Mono<ResponseEntity<ApiResponseWrapper<List<LignePaieDto>>>> findByExercice(@PathVariable UUID exerciceId) {
        return paie_service.findByExercice(exerciceId).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " bulletin(s)")));
    }

    @GetMapping("/periode/{periodeId}")
    @Operation(summary = "Lister les bulletins de paie d'une période")
    public Mono<ResponseEntity<ApiResponseWrapper<List<LignePaieDto>>>> findByPeriode(@PathVariable UUID periodeId) {
        return paie_service.findByPeriode(periodeId).collectList()
            .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, list.size() + " bulletin(s)")));
    }

    @PostMapping("/{id}/comptabiliser")
    @Operation(summary = "Comptabiliser un bulletin de paie",
        description = "Génère automatiquement les écritures OHADA : D6611/C4311/C4441/C4220 pour les salaires, D6613/C4312 pour les charges patronales")
    public Mono<ResponseEntity<ApiResponseWrapper<LignePaieDto>>> comptabiliser(@PathVariable UUID id) {
        return paie_service.comptabiliser(id)
            .map(r -> ResponseEntity.ok(ApiResponseWrapper.success(r, "Bulletin comptabilisé — écritures générées")));
    }
}
