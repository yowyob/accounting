package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.CompteDto;
import com.yowyob.erp.accounting.service.CompteService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comptes")
@RequiredArgsConstructor
@Slf4j
public class CompteController {

    private final CompteService compteService;

    /**
     * Créer un nouveau compte comptable
     */
    @PostMapping
    public ApiResponseWrapper<CompteDto> createCompte(@RequestBody CompteDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("📘 Requête de création d’un compte pour le tenant {}", tenantId);
        CompteDto saved = compteService.createCompte(dto);
        return ApiResponseWrapper.success(saved, "Compte créé avec succès");
    }

    /**
     * Récupérer tous les comptes pour le tenant courant
     */
    @GetMapping
    public ApiResponseWrapper<List<CompteDto>> getAllComptes() {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("📄 Récupération de tous les comptes pour le tenant {}", tenantId);
        List<CompteDto> comptes = compteService.findAllByTenant(tenantId);
        return ApiResponseWrapper.success(comptes, "Liste des comptes récupérée avec succès");
    }

    /**
     * Récupérer un compte par son ID
     */
    @GetMapping("/{id}")
    public ApiResponseWrapper<CompteDto> getCompteById(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("🔍 Récupération du compte ID={} pour le tenant {}", id, tenantId);
        CompteDto compte = compteService.findById(tenantId, id)
                .orElseThrow(() -> new RuntimeException("Compte non trouvé"));
        return ApiResponseWrapper.success(compte, "Compte trouvé");
    }

    /**
     * Rechercher un compte par son numéro de compte
     */
    @GetMapping("/search")
    public ApiResponseWrapper<List<CompteDto>> findByNoCompte(@RequestParam String noCompte) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("🔎 Recherche des comptes avec noCompte={} pour le tenant {}", noCompte, tenantId);
        List<CompteDto> comptes = compteService.findByNoCompte(tenantId, noCompte);
        return ApiResponseWrapper.success(comptes, "Résultats de la recherche");
    }

    /**
     * Mettre à jour un compte existant
     */
    @PutMapping("/{id}")
    public ApiResponseWrapper<CompteDto> updateCompte(@PathVariable UUID id, @RequestBody CompteDto dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("✏️ Mise à jour du compte ID={} pour le tenant {}", id, tenantId);
        CompteDto updated = compteService.updateCompte(tenantId, id, dto);
        return ApiResponseWrapper.success(updated, "Compte mis à jour avec succès");
    }

    /**
     * Supprimer un compte comptable
     */
    @DeleteMapping("/{id}")
    public ApiResponseWrapper<String> deleteCompte(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("🗑️ Suppression du compte ID={} pour le tenant {}", id, tenantId);
        compteService.deleteById(tenantId, id);
        return ApiResponseWrapper.success("Compte supprimé avec succès");
    }
}
