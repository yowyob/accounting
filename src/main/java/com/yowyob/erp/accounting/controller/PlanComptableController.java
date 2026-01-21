package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.PlanComptableDto;
import com.yowyob.erp.accounting.service.PlanComptableService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing the accounting plan (Plan Comptable).
 * Handles CRUD operations, initialization, and searches for accounts.
 * Follows snake_case naming and English Javadoc as per development charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/plan-comptable")
@RequiredArgsConstructor
@Tag(name = "Accounting Plan Comptable", description = "Accounting plan management: creation, update, deactivation, and search.")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class PlanComptableController {

    private final PlanComptableService plan_service;

    /**
     * Initializes the accounting plan for a specific tenant.
     * 
     * @param tenant_id the ID of the tenant to initialize
     * @return success message
     */
    @Operation(summary = "Initialize accounting plan", description = "Creates a default set of accounts for the given tenant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan initialized successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or tenant already initialized")
    })
    @PostMapping("/admin/tenants/{tenantId}/plan-comptable/init-ohada-2025")
    public ResponseEntity<ApiResponseWrapper<String>> initPlanComptable(
            @PathVariable(name = "tenantId") UUID tenant_id) {
        plan_service.initializePlanComptableForTenant(tenant_id);
        return ResponseEntity
                .ok(ApiResponseWrapper.success("OHADA 2025 accounting plan initialized for tenant " + tenant_id));
    }

    /**
     * Creates a new accounting account for the current tenant.
     * 
     * @param dto the account data
     * @return the created account
     */
    @Operation(summary = "Create an accounting account", description = "Creates a new account for the current tenant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully", content = @Content(schema = @Schema(implementation = PlanComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or account already exists")
    })
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<PlanComptableDto>> createPlanComptable(
            @Valid @RequestBody PlanComptableDto dto) {
        try {
            UUID tenant_id = TenantContext.getCurrentTenant();
            log.info("Creating account {} for tenant {}", dto.getNo_compte(), tenant_id);
            PlanComptableDto created = plan_service.createAccount(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseWrapper.success(created, "Accounting account created successfully"));
        } catch (Exception e) {
            log.error("Error creating account: {}", e.getMessage());
            throw new BusinessException("Error during account creation: " + e.getMessage());
        }
    }

    /**
     * Retrieves an account by its ID.
     * 
     * @param id account ID
     * @return the account found
     */
    @Operation(summary = "Get an accounting account by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<PlanComptableDto>> getAccountById(@PathVariable UUID id) {
        log.info("Retrieving account ID={} for current tenant", id);
        try {
            PlanComptableDto dto = plan_service.getAccountById(id);
            return ResponseEntity.ok(ApiResponseWrapper.success(dto, "Account retrieved successfully"));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Accounting account", id.toString());
        }
    }

    /**
     * Lists all accounts for the current tenant.
     * 
     * @return list of accounts
     */
    @Operation(summary = "List all accounting accounts")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>> getAllPlanComptables() {
        log.info("Retrieving all accounts for current tenant");
        List<PlanComptableDto> accounts = plan_service.getAllAccounts();
        return ResponseEntity.ok(ApiResponseWrapper.success(accounts, "Accounts retrieved successfully"));
    }

    /**
     * Lists all active accounts for the current tenant.
     * 
     * @return list of active accounts
     */
    @Operation(summary = "List all active accounts")
    @GetMapping("/actifs")
    public ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>> getActifPlanComptables() {
        log.info("Retrieving active accounts for current tenant");
        List<PlanComptableDto> accounts = plan_service.getAllActiveAccounts();
        return ResponseEntity.ok(ApiResponseWrapper.success(accounts, "Active accounts retrieved successfully"));
    }

    /**
     * Lists accounts by prefix.
     * 
     * @param prefix account number prefix
     * @return list of matching accounts
     */
    @Operation(summary = "List accounts by prefix")
    @GetMapping("/prefix/{prefix}")
    public ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>> getPlanComptablesByPrefix(
            @PathVariable String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponseWrapper.error("Prefix cannot be empty"));
        }
        log.info("Searching for accounts with prefix '{}'", prefix);
        List<PlanComptableDto> accounts = plan_service.getAccountsByPrefix(prefix);
        return ResponseEntity.ok(ApiResponseWrapper.success(accounts, "Accounts retrieved successfully"));
    }

    /**
     * Lists accounts by class.
     * 
     * @param classe account class (1-7)
     * @return list of accounts in the class
     */
    @Operation(summary = "List accounts by class")
    @GetMapping("/classe/{classe}")
    public ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>> getPlanComptablesByClasse(
            @PathVariable Integer classe) {
        if (classe < 1 || classe > 7) {
            return ResponseEntity.badRequest().body(ApiResponseWrapper.error("Class must be between 1 and 7"));
        }
        log.info("Retrieving accounts for class {}", classe);
        List<PlanComptableDto> accounts = plan_service.getAccountsByClass(classe);
        return ResponseEntity.ok(ApiResponseWrapper.success(accounts, "Accounts for class " + classe + " retrieved"));
    }

    /**
     * Updates an existing account.
     * 
     * @param id  account ID
     * @param dto new account data
     * @return updated account
     */
    @Operation(summary = "Update an accounting account")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<PlanComptableDto>> updatePlanComptable(
            @PathVariable UUID id,
            @Valid @RequestBody PlanComptableDto dto) {
        try {
            log.info("Updating account ID={} for current tenant", id);
            PlanComptableDto updated = plan_service.updateAccount(id, dto);
            return ResponseEntity.ok(ApiResponseWrapper.success(updated, "Account updated successfully"));
        } catch (Exception e) {
            log.error("Error updating account {}: {}", id, e.getMessage());
            throw new BusinessException("Error during account update: " + e.getMessage());
        }
    }

    /**
     * Deactivates an account (soft delete).
     * 
     * @param id account ID
     * @return success message
     */
    @Operation(summary = "Deactivate an accounting account", description = "Deactivates an account instead of deleting it.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<String>> deactivatePlanComptable(@PathVariable UUID id) {
        try {
            log.info("Deactivating account ID={} for current tenant", id);
            plan_service.deactivateAccount(id);
            return ResponseEntity.ok(ApiResponseWrapper.success("Account deactivated successfully"));
        } catch (Exception e) {
            log.error("Error deactivating account {}: {}", id, e.getMessage());
            throw new ResourceNotFoundException("Accounting account", id.toString());
        }
    }
}
