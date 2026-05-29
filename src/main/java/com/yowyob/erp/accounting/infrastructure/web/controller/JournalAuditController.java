package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.accounting.application.service.JournalAuditService;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive Controller for retrieving audit logs.
 */
@RestController
@RequestMapping("/api/accounting/audit")
@RequiredArgsConstructor
@Tag(name = "Accounting Audit", description = "Endpoints for retrieving audit logs")
@Slf4j
public class JournalAuditController {

    private final JournalAuditService audit_service;

    /**
     * Retrieves all audits for a organization (last N actions).
     */
    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get all audits for a organization")
    public Mono<ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>>> getAllByOrganization(
            @PathVariable(name = "organizationId") UUID organization_id,
            @RequestParam(defaultValue = "100") int limit) {

        return audit_service.getAllByOrganization(organization_id, limit)
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, "Audit logs retrieved successfully")));
    }

    /**
     * Retrieves audits for a organization within a specific time period.
     */
    @GetMapping("/organization/{organizationId}/periode")
    @Operation(summary = "Get audits by time period")
    public Mono<ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>>> getByPeriode(
            @PathVariable(name = "organizationId") UUID organization_id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        return audit_service.getByPeriode(organization_id, debut, fin)
                .collectList()
                .map(list -> ResponseEntity
                        .ok(ApiResponseWrapper.success(list, "Audit logs for period retrieved successfully")));
    }

    /**
     * Retrieves audits for a organization filtered by user.
     */
    @GetMapping("/organization/{organizationId}/utilisateur/{utilisateur}")
    @Operation(summary = "Get audits by user")
    public Mono<ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>>> getByUtilisateur(
            @PathVariable(name = "organizationId") UUID organization_id,
            @PathVariable String utilisateur) {

        return audit_service.getByUtilisateur(organization_id, utilisateur)
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list,
                        "Audit logs for user " + utilisateur + " retrieved successfully")));
    }

    /**
     * Retrieves audits for a organization filtered by action type.
     */
    @GetMapping("/organization/{organizationId}/action/{action}")
    @Operation(summary = "Get audits by action type")
    public Mono<ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>>> getByAction(
            @PathVariable(name = "organizationId") UUID organization_id,
            @PathVariable String action) {

        return audit_service.getByAction(organization_id, action)
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list,
                        "Audit logs for action " + action + " retrieved successfully")));
    }

    /**
     * Retrieves audits related to a specific accounting entry ID.
     */
    @GetMapping("/organization/{organizationId}/ecriture/{ecritureId}")
    @Operation(summary = "Get audits by accounting entry ID")
    public Mono<ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>>> getByEcriture(
            @PathVariable(name = "organizationId") UUID organization_id,
            @PathVariable(name = "ecritureId") UUID ecriture_id) {

        return audit_service.getByEcriture(organization_id, ecriture_id)
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list,
                        "Audit logs for entry " + ecriture_id + " retrieved successfully")));
    }

    /**
     * Advanced search for audit logs with combined filters.
     */
    @GetMapping("/rechercher")
    @Operation(summary = "Advanced search for audit logs")
    public Mono<ResponseEntity<ApiResponseWrapper<List<JournalAuditDto>>>> rechercher(
            @RequestParam(name = "organizationId") UUID organization_id,
            @RequestParam(required = false) String utilisateur,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        Mono<List<JournalAuditDto>> resultMono;
        if (debut != null && fin != null) {
            resultMono = audit_service.getByPeriode(organization_id, debut, fin).collectList();
        } else if (utilisateur != null && !utilisateur.isBlank()) {
            resultMono = audit_service.getByUtilisateur(organization_id, utilisateur).collectList();
        } else if (action != null && !action.isBlank()) {
            resultMono = audit_service.getByAction(organization_id, action).collectList();
        } else {
            resultMono = audit_service.getAllByOrganization(organization_id, 200).collectList();
        }

        return resultMono
                .map(list -> ResponseEntity.ok(ApiResponseWrapper.success(list, "Audit logs search completed")));
    }
}