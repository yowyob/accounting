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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/plan-comptable")
@RequiredArgsConstructor
@Tag(name = "Plan Comptable", description = "Gestion complète du plan comptable : création, mise à jour, désactivation et recherche.")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class PlanComptableController {

    private final PlanComptableService planComptableService;

    // ✅ INITIALISATION
    @Operation(summary = "iNITIALISER LE PLAN COMPABLE ", description = "Crée unplan compte comptable pour le tenant courant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plan comptable initialisé avec succes",
                    content = @Content(schema = @Schema(implementation = PlanComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Erreur de validation ou compte existant")
    })
    @PostMapping("/admin/tenants/{tenantId}/plan-comptable/init-ohada-2025")
    public ResponseEntity<String> initPlanComptable(@PathVariable UUID tenantId) {
        planComptableService.initialiserPlanComptablePourTenant(tenantId);
        return ResponseEntity.ok("Plan comptable OHADA 2025 initialisé pour le tenant " + tenantId);
    }

    // ✅ CRÉATION
    @Operation(summary = "Créer un compte comptable", description = "Crée un nouveau compte comptable pour le tenant courant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé avec succès",
                    content = @Content(schema = @Schema(implementation = PlanComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Erreur de validation ou compte existant")
    })
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<PlanComptableDto>> createPlanComptable(@Valid @RequestBody PlanComptableDto dto) {
        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            log.info("🧾 Création d’un compte comptable {} pour tenant {}", dto.getNoCompte(), tenantId);
            PlanComptableDto created = planComptableService.createAccount(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseWrapper.success(created, "Compte comptable créé avec succès"));
        } catch (Exception e) {
            log.error("❌ Erreur création compte : {}", e.getMessage());
            throw new BusinessException("Erreur lors de la création du compte : " + e.getMessage());
        }
    }

    // ✅ RÉCUPÉRATION PAR ID
    @Operation(summary = "Récupérer un compte comptable par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<PlanComptableDto>> getAccountById(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("🔍 Consultation du compte ID={} pour tenant {}", id, tenantId);
        try {
            PlanComptableDto dto = planComptableService.getAccountById(id);
            return ResponseEntity.ok(ApiResponseWrapper.success(dto, "Compte trouvé avec succès"));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("PlanComptable", id.toString());
        }
    }

    // ✅ LISTE TOTALE
    @Operation(summary = "Lister tous les comptes comptables")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>> getAllPlanComptables() {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("📋 Récupération de tous les comptes comptables pour tenant {}", tenantId);
        List<PlanComptableDto> accounts = planComptableService.getAllAccounts();
        return ResponseEntity.ok(ApiResponseWrapper.success(accounts, "Liste des comptes récupérée avec succès"));
    }

    // ✅ COMPTES ACTIFS
    @Operation(summary = "Lister tous les comptes comptables actifs")
    @GetMapping("/actifs")
    public ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>> getActifPlanComptables() {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("⚙️ Récupération des comptes actifs pour tenant {}", tenantId);
        List<PlanComptableDto> accounts = planComptableService.getAllActiveAccounts();
        return ResponseEntity.ok(ApiResponseWrapper.success(accounts, "Liste des comptes actifs récupérée"));
    }

    // ✅ RECHERCHE PAR PRÉFIXE
    @Operation(summary = "Lister les comptes par préfixe")
    @GetMapping("/prefix/{prefix}")
    public ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>> getPlanComptablesByPrefix(@PathVariable String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponseWrapper.error("Le préfixe ne peut pas être vide"));
        }
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("🔎 Recherche des comptes commençant par '{}' pour tenant {}", prefix, tenantId);
        List<PlanComptableDto> accounts = planComptableService.getAccountsByPrefix(prefix);
        return ResponseEntity.ok(ApiResponseWrapper.success(accounts, "Comptes récupérés avec succès"));
    }

    // ✅ RECHERCHE PAR CLASSE
    @Operation(summary = "Lister les comptes par classe")
    @GetMapping("/classe/{classe}")
    public ResponseEntity<ApiResponseWrapper<List<PlanComptableDto>>> getPlanComptablesByClasse(@PathVariable Integer classe) {
        if (classe < 1 || classe > 7) {
            return ResponseEntity.badRequest().body(ApiResponseWrapper.error("La classe doit être comprise entre 1 et 7"));
        }
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("📚 Récupération des comptes de la classe {} pour tenant {}", classe, tenantId);
        List<PlanComptableDto> accounts = planComptableService.getAccountsByClass(classe);
        return ResponseEntity.ok(ApiResponseWrapper.success(accounts, "Comptes de la classe " + classe + " récupérés"));
    }

    // ✅ MISE À JOUR
    @Operation(summary = "Mettre à jour un compte comptable")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<PlanComptableDto>> updatePlanComptable(
            @PathVariable UUID id,
            @Valid @RequestBody PlanComptableDto dto) {
        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            log.info("✏️ Mise à jour du compte ID={} pour tenant {}", id, tenantId);
            PlanComptableDto updated = planComptableService.updateAccount(id, dto);
            return ResponseEntity.ok(ApiResponseWrapper.success(updated, "Compte comptable mis à jour avec succès"));
        } catch (Exception e) {
            log.error("Erreur mise à jour compte {} : {}", id, e.getMessage());
            throw new BusinessException("Erreur mise à jour du compte : " + e.getMessage());
        }
    }

    // ✅ DÉSACTIVATION
    @Operation(summary = "Désactiver un compte comptable", description = "Désactive un compte comptable au lieu de le supprimer.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<String>> deactivatePlanComptable(@PathVariable UUID id) {
        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            log.info("🗑️ Désactivation du compte comptable ID={} pour tenant {}", id, tenantId);
            planComptableService.deactivateAccount(id);
            return ResponseEntity.ok(ApiResponseWrapper.success("Compte comptable désactivé avec succès"));
        } catch (Exception e) {
            log.error("Erreur désactivation compte {} : {}", id, e.getMessage());
            throw new ResourceNotFoundException("PlanComptable", id.toString());
        }
    }
}
