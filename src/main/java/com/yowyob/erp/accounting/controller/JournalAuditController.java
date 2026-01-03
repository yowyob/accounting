package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.JournalAuditDto;
import com.yowyob.erp.accounting.service.JournalAuditService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller for retrieving audit logs.
 * Provides endpoints for filtering audit trails by tenant, period, user, and
 * action.
 * 
 * @author ALD
 * @date 30.09.25
 */
@RestController
@RequestMapping("/api/accounting/audit")
@RequiredArgsConstructor
@Tag(name = "Accounting Audit", description = "Endpoints for retrieving audit logs")
public class JournalAuditController {

    private final JournalAuditService audit_service;

    /**
     * Retrieves all audits for a tenant (last N actions).
     * 
     * @param tenant_id the tenant ID
     * @param limit     maximum results (default 100)
     * @return list of audit DTOs
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<JournalAuditDto>> getAllByTenant(
            @PathVariable(name = "tenantId") UUID tenant_id,
            @RequestParam(defaultValue = "100") int limit) {

        return ResponseEntity.ok(audit_service.getAllByTenant(tenant_id, limit));
    }

    /**
     * Retrieves audits for a tenant within a specific time period.
     * 
     * @param tenant_id the tenant ID
     * @param debut     start date-time
     * @param fin       end date-time
     * @return list of audit DTOs
     */
    @GetMapping("/tenant/{tenantId}/periode")
    public ResponseEntity<List<JournalAuditDto>> getByPeriode(
            @PathVariable(name = "tenantId") UUID tenant_id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        return ResponseEntity.ok(audit_service.getByPeriode(tenant_id, debut, fin));
    }

    /**
     * Retrieves audits for a tenant filtered by user.
     * 
     * @param tenant_id   the tenant ID
     * @param utilisateur the username
     * @return list of audit DTOs
     */
    @GetMapping("/tenant/{tenantId}/utilisateur/{utilisateur}")
    public ResponseEntity<List<JournalAuditDto>> getByUtilisateur(
            @PathVariable(name = "tenantId") UUID tenant_id,
            @PathVariable String utilisateur) {

        return ResponseEntity.ok(audit_service.getByUtilisateur(tenant_id, utilisateur));
    }

    /**
     * Retrieves audits for a tenant filtered by action type.
     * 
     * @param tenant_id the tenant ID
     * @param action    the action (e.g., CREATE, VALIDATE)
     * @return list of audit DTOs
     */
    @GetMapping("/tenant/{tenantId}/action/{action}")
    public ResponseEntity<List<JournalAuditDto>> getByAction(
            @PathVariable(name = "tenantId") UUID tenant_id,
            @PathVariable String action) {

        return ResponseEntity.ok(audit_service.getByAction(tenant_id, action));
    }

    /**
     * Retrieves audits related to a specific accounting entry ID.
     * 
     * @param tenant_id   the tenant ID
     * @param ecriture_id the entry ID
     * @return list of audit DTOs
     */
    @GetMapping("/tenant/{tenantId}/ecriture/{ecritureId}")
    public ResponseEntity<List<JournalAuditDto>> getByEcriture(
            @PathVariable(name = "tenantId") UUID tenant_id,
            @PathVariable(name = "ecritureId") UUID ecriture_id) {

        return ResponseEntity.ok(audit_service.getByEcriture(tenant_id, ecriture_id));
    }

    /**
     * Advanced search for audit logs with combined filters.
     * 
     * @param tenant_id   the tenant ID
     * @param utilisateur optional user filter
     * @param action      optional action filter
     * @param debut       optional start date filter
     * @param fin         optional end date filter
     * @return list of audit DTOs
     */
    @GetMapping("/rechercher")
    public ResponseEntity<List<JournalAuditDto>> rechercher(
            @RequestParam(name = "tenantId") UUID tenant_id,
            @RequestParam(required = false) String utilisateur,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        if (debut != null && fin != null) {
            return ResponseEntity.ok(audit_service.getByPeriode(tenant_id, debut, fin));
        } else if (utilisateur != null && !utilisateur.isBlank()) {
            return ResponseEntity.ok(audit_service.getByUtilisateur(tenant_id, utilisateur));
        } else if (action != null && !action.isBlank()) {
            return ResponseEntity.ok(audit_service.getByAction(tenant_id, action));
        } else {
            return ResponseEntity.ok(audit_service.getAllByTenant(tenant_id, 200));
        }
    }
}