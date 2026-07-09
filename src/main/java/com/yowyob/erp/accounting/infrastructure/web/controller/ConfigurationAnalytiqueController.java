package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.ConfigurationAnalytiqueService;
import com.yowyob.erp.accounting.infrastructure.web.dto.ConfigurationAnalytiqueDto;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/accounting/analytique/config")
@RequiredArgsConstructor
@Tag(name = "Configuration Analytique", description = "Paramétrage global de la comptabilité analytique")
@SecurityRequirement(name = "BasicAuth")
public class ConfigurationAnalytiqueController {

    private final ConfigurationAnalytiqueService service;

    @GetMapping
    @Operation(summary = "Obtenir la configuration analytique de l'organisation")
    public Mono<ResponseEntity<ApiResponseWrapper<ConfigurationAnalytiqueDto>>> get() {
        return service.get()
            .map(config -> ResponseEntity.ok(
                ApiResponseWrapper.success(config, "Configuration analytique chargée")));
    }

    @PutMapping
    @Operation(summary = "Enregistrer la configuration analytique")
    public Mono<ResponseEntity<ApiResponseWrapper<ConfigurationAnalytiqueDto>>> save(
            @Valid @RequestBody ConfigurationAnalytiqueDto dto) {
        return service.save(dto)
            .map(config -> ResponseEntity.ok(
                ApiResponseWrapper.success(config, "Configuration analytique enregistrée")));
    }
}
