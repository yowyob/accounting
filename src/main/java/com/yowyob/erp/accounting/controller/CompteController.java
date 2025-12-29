package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.CompteDto;
import com.yowyob.erp.accounting.service.CompteService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.common.exception.ResourceNotFoundException;
import com.yowyob.erp.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing accounting accounts.
 * Provides REST endpoints for CRUD operations on Compte entities.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/comptes")
@RequiredArgsConstructor
@Slf4j
public class CompteController {

    private final CompteService compte_service;

    /**
     * Creates a new accounting account.
     * 
     * @param dto the account data
     * @return the created account wrapped in ApiResponseWrapper
     */
    @PostMapping
    public ApiResponseWrapper<CompteDto> createCompte(@RequestBody CompteDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Request to create an account for tenant {}", tenant_id);
        CompteDto saved = compte_service.createCompte(dto);
        return ApiResponseWrapper.success(saved, "Account created successfully");
    }

    /**
     * Retrieves all accounts for the current tenant.
     * 
     * @return list of accounts wrapped in ApiResponseWrapper
     */
    @GetMapping
    public ApiResponseWrapper<List<CompteDto>> getAllComptes() {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Retrieving all accounts for tenant {}", tenant_id);
        List<CompteDto> comptes = compte_service.findAllByTenant(tenant_id);
        return ApiResponseWrapper.success(comptes, "Accounts list retrieved successfully");
    }

    /**
     * Retrieves an account by its ID.
     * 
     * @param id the account ID
     * @return the account wrapped in ApiResponseWrapper
     */
    @GetMapping("/{id}")
    public ApiResponseWrapper<CompteDto> getCompteById(@PathVariable UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Retrieving account ID={} for tenant {}", id, tenant_id);
        CompteDto compte = compte_service.findById(tenant_id, id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting account", id.toString()));
        return ApiResponseWrapper.success(compte, "Account found");
    }

    /**
     * Searches for accounts by account number.
     * 
     * @param no_compte the account number to search for
     * @return list of matching accounts wrapped in ApiResponseWrapper
     */
    @GetMapping("/search")
    public ApiResponseWrapper<List<CompteDto>> findByNoCompte(@RequestParam String no_compte) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Searching accounts with no_compte={} for tenant {}", no_compte, tenant_id);
        List<CompteDto> comptes = compte_service.findByNoCompte(tenant_id, no_compte);
        return ApiResponseWrapper.success(comptes, "Search results");
    }

    /**
     * Updates an existing account.
     * 
     * @param id  the account ID
     * @param dto the new account data
     * @return the updated account wrapped in ApiResponseWrapper
     */
    @PutMapping("/{id}")
    public ApiResponseWrapper<CompteDto> updateCompte(@PathVariable UUID id, @RequestBody CompteDto dto) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Updating account ID={} for tenant {}", id, tenant_id);
        CompteDto updated = compte_service.updateCompte(tenant_id, id, dto);
        return ApiResponseWrapper.success(updated, "Account updated successfully");
    }

    /**
     * Deletes an accounting account.
     * 
     * @param id the account ID to delete
     * @return success message wrapped in ApiResponseWrapper
     */
    @DeleteMapping("/{id}")
    public ApiResponseWrapper<String> deleteCompte(@PathVariable UUID id) {
        UUID tenant_id = TenantContext.getCurrentTenant();
        log.info("Deleting account ID={} for tenant {}", id, tenant_id);
        compte_service.deleteById(tenant_id, id);
        return ApiResponseWrapper.success("Account deleted successfully");
    }
}
