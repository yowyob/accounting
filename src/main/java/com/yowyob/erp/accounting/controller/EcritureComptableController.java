package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.service.EcritureComptableService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.common.dto.ComptableObjectRequest;
import com.yowyob.erp.common.entity.ComptableObject;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.yowyob.erp.accounting.util.AccountingUtils.mapToComptableObject;

@RestController
@RequestMapping("/api/accounting/entries")
@RequiredArgsConstructor
@Tag(name = "Écritures Comptables", description = "Gestion complète des écritures comptables avec Kafka + Redis + multitenant")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class EcritureComptableController {

    private final EcritureComptableService ecritureService;

    // ✅ CRÉATION D’UNE ÉCRITURE
    @Operation(summary = "Créer manuellement une écriture comptable",
            description = "Crée une nouvelle écriture comptable après validation de la période et du journal.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Écriture créée avec succès",
                    content = @Content(schema = @Schema(implementation = EcritureComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Erreur de validation des données")
    })
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<EcritureComptableDto>> createEcriture(
            @Valid @RequestBody EcritureComptableDto ecritureDto) {
        try {
            EcritureComptableDto created = ecritureService.createEcriture(ecritureDto);
            log.info("🧾 Écriture créée : {}", created.getNumeroEcriture());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseWrapper.success(created, "Écriture comptable créée avec succès"));
        } catch (Exception e) {
            log.error("Erreur création écriture : {}", e.getMessage());
            throw new BusinessException("Erreur lors de la création : " + e.getMessage());
        }
    }

    // ✅ VALIDATION
    @Operation(summary = "Valider une écriture comptable")
    @PostMapping("/{id}/validate")
    public ResponseEntity<ApiResponseWrapper<EcritureComptableDto>> validateEcriture(
            @PathVariable UUID id, Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "system";
        EcritureComptableDto validated = ecritureService.validateEcriture(id, user);
        log.info("✅ Écriture validée par {}", user);
        return ResponseEntity.ok(ApiResponseWrapper.success(validated, "Écriture comptable validée"));
    }

    // ✅ RÉCUPÉRATION DE TOUTES LES ÉCRITURES
    @Operation(summary = "Lister toutes les écritures comptables")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<List<EcritureComptableDto>>> getAllEcritures() {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("📄 Récupération de toutes les écritures pour tenant {}", tenantId);
        List<EcritureComptableDto> ecritures = ecritureService.getAll();
        return ResponseEntity.ok(ApiResponseWrapper.success(ecritures));
    }

    // ✅ DÉTAIL D’UNE ÉCRITURE
    @Operation(summary = "Récupérer une écriture comptable par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<EcritureComptableDto>> getEcritureById(@PathVariable UUID id) {
        EcritureComptableDto ecriture = ecritureService.getById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Écriture comptable", id.toString()));
        log.info("🔍 Écriture récupérée : {}", id);
        return ResponseEntity.ok(ApiResponseWrapper.success(ecriture));
    }

    // ✅ NON VALIDÉES
    @Operation(summary = "Lister les écritures non validées")
    @GetMapping("/non-validated")
    public ResponseEntity<ApiResponseWrapper<List<EcritureComptableDto>>> getNonValidatedEcritures() {
        List<EcritureComptableDto> ecritures = ecritureService.getNonValidated();
        log.info("⏳ {} écritures non validées récupérées", ecritures.size());
        return ResponseEntity.ok(ApiResponseWrapper.success(ecritures));
    }

    // ✅ RECHERCHE PAR DATE ET JOURNAL
    @Operation(summary = "Rechercher des écritures par période et journal")
    @GetMapping("/search")
    public ResponseEntity<ApiResponseWrapper<List<EcritureComptableDto>>> searchEcritures(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) UUID journalId) {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseWrapper.error("La date de début doit précéder la date de fin"));
        }

        List<EcritureComptableDto> ecritures = ecritureService.searchEcritures(startDate, endDate, journalId);
        log.info("🔎 Recherche d’écritures entre {} et {} pour journal {}", startDate, endDate, journalId);
        return ResponseEntity.ok(ApiResponseWrapper.success(ecritures, "Recherche effectuée"));
    }

    // ✅ GÉNÉRATION AUTOMATIQUE À PARTIR D’UN OBJET COMPTABLE
    @Operation(summary = "Générer une écriture depuis un objet comptable")
    @PostMapping("/generate-from-object")
    public ResponseEntity<ApiResponseWrapper<EcritureComptableDto>> generateFromComptableObject(
            @RequestBody ComptableObjectRequest request) {
        try {
            if (request.getTenantId() == null || request.getJournalComptableId() == null) {
                throw new BusinessException("Tenant ID et Journal Comptable ID sont requis");
            }
            ComptableObject object = mapToComptableObject(request);
            EcritureComptableDto generated = ecritureService.generateFromComptableObject(object);
            log.info("⚙️ Écriture générée automatiquement pour objet {}", object.getSourceType());
            return ResponseEntity.ok(ApiResponseWrapper.success(generated, "Écriture générée avec succès"));
        } catch (Exception e) {
            log.error("Erreur génération écriture automatique : {}", e.getMessage());
            throw new BusinessException("Erreur lors de la génération : " + e.getMessage());
        }
    }

    // ✅ SUPPRESSION
    @Operation(summary = "Supprimer une écriture comptable (si non validée)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteEcriture(@PathVariable UUID id) {
        try {
            ecritureService.deleteEcriture(id);
            log.info("🗑️ Écriture supprimée {}", id);
            return ResponseEntity.ok(ApiResponseWrapper.success(null, "Écriture supprimée avec succès"));
        } catch (IllegalStateException e) {
            throw new BusinessException("Écriture déjà validée : " + e.getMessage());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Écriture non trouvée", id.toString());
        }
    }
}
