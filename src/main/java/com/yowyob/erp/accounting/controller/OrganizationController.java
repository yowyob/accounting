package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.OrganizationDto;
import com.yowyob.erp.accounting.service.OrganizationService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive REST Controller for managing Organizations.
 */
@RestController
@RequestMapping("/api/accounting/organizations")
@RequiredArgsConstructor
@Tag(name = "Accounting Organizations", description = "Endpoints for top-level organization management")
public class OrganizationController {

    private final OrganizationService organization_service;

    @PostMapping
    @Operation(summary = "Create a new organization")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponseWrapper<OrganizationDto>> createOrganization(
            @RequestBody OrganizationDto organization_dto) {
        return organization_service.createOrganization(organization_dto)
                .map(created -> ApiResponseWrapper.success(created, "Organization created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    public Mono<ApiResponseWrapper<OrganizationDto>> getOrganization(@PathVariable UUID id) {
        return organization_service.getOrganization(id)
                .map(organization -> ApiResponseWrapper.success(organization, "Organization found"));
    }

    @GetMapping
    @Operation(summary = "Get all organizations")
    public Mono<ApiResponseWrapper<java.util.List<OrganizationDto>>> getAllOrganizations() {
        return organization_service.getAllOrganizations()
                .collectList()
                .map(organizations -> ApiResponseWrapper.success(organizations, "Organizations list retrieved"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an organization")
    public Mono<ApiResponseWrapper<OrganizationDto>> updateOrganization(@PathVariable UUID id,
            @RequestBody OrganizationDto organization_dto) {
        return organization_service.updateOrganization(id, organization_dto)
                .map(updated -> ApiResponseWrapper.success(updated, "Organization updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an organization")
    public Mono<ApiResponseWrapper<Void>> deleteOrganization(@PathVariable UUID id) {
        return organization_service.deleteOrganization(id)
                .thenReturn(ApiResponseWrapper.success(null, "Organization deleted successfully"));
    }
}
