package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.OperationComptableDto;
import com.yowyob.erp.accounting.service.OperationComptableService;
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
@RequestMapping("/api/accounting/operations")
@RequiredArgsConstructor
@Tag(name = "Opérations Comptables", description = "Gestion complète des opérations comptables avec Kafka, Redis et multitenant")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class OperationComptableController {

    private final OperationComptableService operationComptableService;

    // ✅ CRÉATION
    @Operation(summary = "Créer une opération comptable", description = "Crée une nouvelle opération comptable pour le tenant courant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Opération créée avec succès",
                    content = @Content(schema = @Schema(implementation = OperationComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Erreur de validation des données")
    })
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<OperationComptableDto>> createOperationComptable(
            @Valid @RequestBody OperationComptableDto dto) {
        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            log.info("🧾 Création d’une opération comptable pour tenant {}", tenantId);
            OperationComptableDto created = operationComptableService.createOperation(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseWrapper.success(created, "Opération comptable créée avec succès"));
        } catch (Exception e) {
            log.error("❌ Erreur création opération : {}", e.getMessage());
            throw new BusinessException("Erreur lors de la création : " + e.getMessage());
        }
    }

    // ✅ LECTURE PAR ID
    @Operation(summary = "Récupérer une opération comptable par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opération trouvée"),
            @ApiResponse(responseCode = "404", description = "Non trouvée")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<OperationComptableDto>> getOperationComptable(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("🔍 Récupération de l’opération comptable ID={} pour tenant {}", id, tenantId);
        return operationComptableService.getOperation(id)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Opération trouvée")))
                .orElseThrow(() -> new ResourceNotFoundException("OperationComptable", id.toString()));
    }

    // ✅ LISTER TOUTES LES OPÉRATIONS
    @Operation(summary = "Lister toutes les opérations comptables")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<List<OperationComptableDto>>> getAllOperationsComptables() {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("📋 Récupération de toutes les opérations comptables pour tenant {}", tenantId);
        List<OperationComptableDto> operations = operationComptableService.getAllOperations();
        return ResponseEntity.ok(ApiResponseWrapper.success(operations, "Liste des opérations comptables récupérée"));
    }

    // ✅ RECHERCHE PAR COMPTE
    @Operation(summary = "Récupérer les opérations comptables d’un compte principal")
    @GetMapping("/by-no-compte")
    public ResponseEntity<ApiResponseWrapper<List<OperationComptableDto>>> getOperationsByNoCompte(
            @RequestParam String noCompte) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("🔎 Récupération des opérations par compte={} pour tenant {}", noCompte, tenantId);
        List<OperationComptableDto> operations = operationComptableService.getOperationsByCompte(noCompte);
        return ResponseEntity.ok(ApiResponseWrapper.success(operations, "Opérations comptables récupérées"));
    }

    // ✅ RECHERCHE PAR TYPE + MODE
    @Operation(summary = "Rechercher une opération par type et mode de règlement")
    @GetMapping("/search")
    public ResponseEntity<ApiResponseWrapper<OperationComptableDto>> getOperationByTypeAndMode(
            @RequestParam String typeOperation,
            @RequestParam String modeReglement) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("🔎 Recherche opération type={} / mode={} pour tenant {}", typeOperation, modeReglement, tenantId);
        return operationComptableService.getByTypeAndMode(typeOperation, modeReglement)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Opération trouvée")))
                .orElseThrow(() -> new ResourceNotFoundException("OperationComptable", typeOperation + "-" + modeReglement));
    }

    // ✅ MISE À JOUR
    @Operation(summary = "Mettre à jour une opération comptable")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<OperationComptableDto>> updateOperationComptable(
            @PathVariable UUID id,
            @Valid @RequestBody OperationComptableDto dto) {
        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            log.info("✏️ Mise à jour de l’opération comptable ID={} pour tenant {}", id, tenantId);
            OperationComptableDto updated = operationComptableService.updateOperation(id, dto);
            return ResponseEntity.ok(ApiResponseWrapper.success(updated, "Opération comptable mise à jour avec succès"));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("OperationComptable", id.toString());
        } catch (Exception e) {
            log.error("Erreur mise à jour opération {} : {}", id, e.getMessage());
            throw new BusinessException("Erreur mise à jour : " + e.getMessage());
        }
    }

    // ✅ SUPPRESSION
    @Operation(summary = "Supprimer une opération comptable", description = "Supprime une opération comptable existante par ID.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<String>> deleteOperationComptable(@PathVariable UUID id) {
        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            log.info("🗑️ Suppression de l’opération ID={} pour tenant {}", id, tenantId);
            operationComptableService.deleteOperation(id);
            return ResponseEntity.ok(ApiResponseWrapper.success("Opération comptable supprimée avec succès"));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("OperationComptable", id.toString());
        } catch (Exception e) {
            log.error("Erreur suppression opération {} : {}", id, e.getMessage());
            throw new BusinessException("Erreur lors de la suppression : " + e.getMessage());
        }
    }
}
