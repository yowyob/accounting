package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.infrastructure.web.dto.AgenceDto;
import com.yowyob.erp.accounting.domain.port.in.AgenceUseCase;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * REST Controller for managing Agencies (Agences).
 * 
 * @author ALD
 * @date 03.01.2026
 */
@RestController
@RequestMapping("/api/accounting/agences")
@RequiredArgsConstructor
@Tag(name = "Accounting Agences", description = "Endpoints for branch/agency management")
public class AgenceController {

    private final AgenceUseCase agence_service;

    @PostMapping
    @Operation(summary = "Create a new agency")
    public Mono<ResponseEntity<ApiResponseWrapper<AgenceDto>>> createAgence(@RequestBody AgenceDto agence_dto) {
        return agence_service.createAgence(agence_dto)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponseWrapper.success(created, "Agency created successfully")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get agency by ID")
    public Mono<ResponseEntity<ApiResponseWrapper<AgenceDto>>> getAgence(@PathVariable UUID id) {
        return agence_service.getAgence(id)
                .map(agence -> ResponseEntity.ok(ApiResponseWrapper.success(agence, "Agency found")));
    }

    @GetMapping
    @Operation(summary = "Get all agencies for current organization")
    public Mono<ResponseEntity<ApiResponseWrapper<List<AgenceDto>>>> getAllAgences() {
        return agence_service.getAllAgences()
                .collectList()
                .map(agences -> ResponseEntity.ok(ApiResponseWrapper.success(agences, "Agencies list retrieved")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an agency")
    public Mono<ResponseEntity<ApiResponseWrapper<AgenceDto>>> updateAgence(@PathVariable UUID id,
            @RequestBody AgenceDto agence_dto) {
        return agence_service.updateAgence(id, agence_dto)
                .map(updated -> ResponseEntity.ok(ApiResponseWrapper.success(updated, "Agency updated successfully")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an agency")
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> deleteAgence(@PathVariable UUID id) {
        return agence_service.deleteAgence(id)
                .then(Mono.fromCallable(() -> ResponseEntity.ok(ApiResponseWrapper.success(null, "Agency deleted successfully"))));
    }
}
