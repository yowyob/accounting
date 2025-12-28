package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.entity.JournalAudit;
import com.yowyob.erp.accounting.repository.JournalAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/audit")
@RequiredArgsConstructor
public class JournalAuditController {

    private final JournalAuditRepository auditRepository;

    // 1. Toutes les actions du tenant (les 100 dernières par défaut)
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<JournalAudit>> getAllByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "100") int limit) {

        List<JournalAudit> audits = auditRepository.findByTenant_IdOrderByDateActionDesc(tenantId)
                .stream()
                .limit(limit)
                .toList();

        return ResponseEntity.ok(audits);
    }

    // 2. Par période précise
    @GetMapping("/tenant/{tenantId}/periode")
    public ResponseEntity<List<JournalAudit>> getByPeriode(
            @PathVariable UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        List<JournalAudit> audits = auditRepository.findByTenant_IdAndDateActionBetween(tenantId, debut, fin);
        return ResponseEntity.ok(audits);
    }

    // 3. Par utilisateur
    @GetMapping("/tenant/{tenantId}/utilisateur/{utilisateur}")
    public ResponseEntity<List<JournalAudit>> getByUtilisateur(
            @PathVariable UUID tenantId,
            @PathVariable String utilisateur) {

        List<JournalAudit> audits = auditRepository.findByTenant_IdAndUtilisateur(tenantId, utilisateur);
        return ResponseEntity.ok(audits);
    }

    // 4. Par action (CREATE, VALIDATE, LETTRAGE, CLOTURE, etc.)
    @GetMapping("/tenant/{tenantId}/action/{action}")
    public ResponseEntity<List<JournalAudit>> getByAction(
            @PathVariable UUID tenantId,
            @PathVariable String action) {

        List<JournalAudit> audits = auditRepository.findByTenant_IdAndAction(tenantId, action);
        return ResponseEntity.ok(audits);
    }

    // 5. Par écriture comptable précise (très utile pour traçabilité)
    @GetMapping("/tenant/{tenantId}/ecriture/{ecritureId}")
    public ResponseEntity<List<JournalAudit>> getByEcriture(
            @PathVariable UUID tenantId,
            @PathVariable UUID ecritureId) {

        List<JournalAudit> audits = auditRepository.findByTenant_IdAndEcritureComptableId(tenantId, ecritureId);
        return ResponseEntity.ok(audits);
    }

    // 6. Recherche avancée combinée (le plus puissant)
    @GetMapping("/rechercher")
    public ResponseEntity<List<JournalAudit>> rechercher(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String utilisateur,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        List<JournalAudit> audits;

        if (debut != null && fin != null) {
            audits = auditRepository.findByTenant_IdAndDateActionBetween(tenantId, debut, fin);
        } else if (utilisateur != null && !utilisateur.isBlank()) {
            audits = auditRepository.findByTenant_IdAndUtilisateur(tenantId, utilisateur);
        } else if (action != null && !action.isBlank()) {
            audits = auditRepository.findByTenant_IdAndAction(tenantId, action);
        } else {
            audits = auditRepository.findByTenant_IdOrderByDateActionDesc(tenantId)
                    .stream().limit(200).toList();
        }

        return ResponseEntity.ok(audits);
    }
}