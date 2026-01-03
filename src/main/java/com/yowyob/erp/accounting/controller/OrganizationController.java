package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.OrganizationDto;
import com.yowyob.erp.accounting.service.OrganizationService;
import com.yowyob.erp.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing Organizations.
 * 
 * @author ALD
 * @date 03.01.2026
 */
@RestController
@RequestMapping("/api/accounting/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Endpoints for top-level organization management")
public class OrganizationController {

    private final OrganizationService organization_service;

    @PostMapping
    @Operation(summary = "Create a new organization")
    public ResponseEntity<ApiResponse<OrganizationDto>> createOrganization(
            @RequestBody OrganizationDto organization_dto) {
        OrganizationDto created = organization_service.createOrganization(organization_dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Organization created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    public ResponseEntity<ApiResponse<OrganizationDto>> getOrganization(@PathVariable UUID id) {
        OrganizationDto organization = organization_service.getOrganization(id);
        return ResponseEntity.ok(ApiResponse.success(organization));
    }

    @GetMapping
    @Operation(summary = "Get all organizations")
    public ResponseEntity<ApiResponse<List<OrganizationDto>>> getAllOrganizations() {
        List<OrganizationDto> organizations = organization_service.getAllOrganizations();
        return ResponseEntity.ok(ApiResponse.success(organizations));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an organization")
    public ResponseEntity<ApiResponse<OrganizationDto>> updateOrganization(@PathVariable UUID id,
            @RequestBody OrganizationDto organization_dto) {
        OrganizationDto updated = organization_service.updateOrganization(id, organization_dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Organization updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an organization")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable UUID id) {
        organization_service.deleteOrganization(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Organization deleted successfully"));
    }
}
