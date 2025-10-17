package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.PeriodeComptableDto;
import com.yowyob.erp.accounting.service.PeriodeComptableService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/periodes")
@RequiredArgsConstructor
@Tag(name = "Périodes Comptables", description = "Gestion complète des périodes comptables : création, mise à jour, clôture et suppression.")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class PeriodeComptableController {

    private final PeriodeComptableService periodeComptableService;

    // ✅ CRÉATION
    @Operation(summary = "Créer une période comptable", description = "Crée une nouvelle période comptable pour le tenant courant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Période créée avec succès",
                    content = @Content(schema = @Schema(implementation = PeriodeComptableDto.class))),
            @ApiResponse(responseCode = "400", description = "Erreur de validation ou période chevauchante")
    })
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> createPeriodeComptable(
            @Valid @RequestBody PeriodeComptableDto dto) {
        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            log.info("🧾 Création d’une période comptable pour tenant {}", tenantId);
            PeriodeComptableDto created = periodeComptableService.createPeriode(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseWrapper.success(created, "Période comptable créée avec succès"));
        } catch (Exception e) {
            log.error("❌ Erreur création période : {}", e.getMessage());
            throw new BusinessException("Erreur création période : " + e.getMessage());
        }
    }

    // ✅ RÉCUPÉRATION PAR ID
    @Operation(summary = "Récupérer une période comptable par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> getPeriodeComptable(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("🔍 Consultation de la période ID={} pour tenant {}", id, tenantId);
        return periodeComptableService.getPeriode(id)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Période comptable trouvée")))
                .orElseThrow(() -> new ResourceNotFoundException("PeriodeComptable", id.toString()));
    }

    // ✅ LISTE TOTALE
    @Operation(summary = "Lister toutes les périodes comptables")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<List<PeriodeComptableDto>>> getAllPeriodeComptables() {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("📋 Récupération de toutes les périodes comptables pour tenant {}", tenantId);
        List<PeriodeComptableDto> periodes = periodeComptableService.getAllPeriodes();
        return ResponseEntity.ok(ApiResponseWrapper.success(periodes, "Liste complète des périodes comptables récupérée"));
    }

    // ✅ PAR CODE
    @Operation(summary = "Récupérer une période par code")
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> getPeriodeByCode(@PathVariable String code) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("🔎 Récupération de la période par code={} pour tenant {}", code, tenantId);
        return periodeComptableService.getByCode(code)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Période comptable trouvée")))
                .orElseThrow(() -> new ResourceNotFoundException("PeriodeComptable", code));
    }

    // ✅ PAR DATE
    @Operation(summary = "Récupérer une période comptable par date")
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> getPeriodeByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("📅 Recherche de période contenant la date {} pour tenant {}", date, tenantId);
        return periodeComptableService.getByDate(date)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Période contenant la date trouvée")))
                .orElseThrow(() -> new ResourceNotFoundException("PeriodeComptable", date.toString()));
    }

    // ✅ PÉRIODES NON CLÔTURÉES
    @Operation(summary = "Lister les périodes non clôturées")
    @GetMapping("/non-closed")
    public ResponseEntity<ApiResponseWrapper<List<PeriodeComptableDto>>> getNonClosedPeriodes() {
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("⏳ Récupération des périodes non clôturées pour tenant {}", tenantId);
        List<PeriodeComptableDto> periodes = periodeComptableService.getNonClosedPeriodes();
        return ResponseEntity.ok(ApiResponseWrapper.success(periodes, "Périodes non clôturées récupérées"));
    }

    // ✅ PÉRIODES PAR PLAGE
    @Operation(summary = "Lister les périodes entre deux dates")
    @GetMapping("/range")
    public ResponseEntity<ApiResponseWrapper<List<PeriodeComptableDto>>> getPeriodesByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseWrapper.error("La date de début doit être antérieure à la date de fin"));
        }
        UUID tenantId = TenantContext.getCurrentTenant();
        log.info("📆 Récupération des périodes entre {} et {} pour tenant {}", startDate, endDate, tenantId);
        List<PeriodeComptableDto> periodes = periodeComptableService.getByRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponseWrapper.success(periodes, "Périodes récupérées avec succès"));
    }

    // ✅ MISE À JOUR
    @Operation(summary = "Mettre à jour une période comptable")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> updatePeriodeComptable(
            @PathVariable UUID id,
            @Valid @RequestBody PeriodeComptableDto dto) {
        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            log.info("✏️ Mise à jour de la période ID={} pour tenant {}", id, tenantId);
            PeriodeComptableDto updated = periodeComptableService.updatePeriode(id, dto);
            return ResponseEntity.ok(ApiResponseWrapper.success(updated, "Période comptable mise à jour avec succès"));
        } catch (Exception e) {
            log.error("Erreur mise à jour période {} : {}", id, e.getMessage());
            throw new BusinessException("Erreur mise à jour période : " + e.getMessage());
        }
    }

    // ✅ CLÔTURE
    @Operation(summary = "Clôturer une période comptable")
    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponseWrapper<PeriodeComptableDto>> closePeriodeComptable(@PathVariable UUID id) {
        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            log.info("🔒 Clôture de la période ID={} pour tenant {}", id, tenantId);
            PeriodeComptableDto closed = periodeComptableService.closePeriode(id);
            return ResponseEntity.ok(ApiResponseWrapper.success(closed, "Période clôturée avec succès"));
        } catch (IllegalStateException e) {
            throw new BusinessException("Période déjà clôturée : " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur clôture période {} : {}", id, e.getMessage());
            throw new BusinessException("Erreur clôture période : " + e.getMessage());
        }
    }

    // ✅ SUPPRESSION
    @Operation(summary = "Supprimer une période comptable")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<String>> deletePeriodeComptable(@PathVariable UUID id) {
        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            log.info("🗑️ Suppression de la période ID={} pour tenant {}", id, tenantId);
            periodeComptableService.deletePeriode(id);
            return ResponseEntity.ok(ApiResponseWrapper.success("Période comptable supprimée avec succès"));
        } catch (IllegalStateException e) {
            throw new BusinessException("Impossible de supprimer une période clôturée : " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur suppression période {} : {}", id, e.getMessage());
            throw new ResourceNotFoundException("PeriodeComptable", id.toString());
        }
    }
}
