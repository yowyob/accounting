package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contrepartie d'une opération comptable (Phase 2 du paramétrage)
 * Modélise les comptes à mouvementer pour une opération comptable.
 * Jointures vers JournalComptable et OperationComptable pour intégrité relationnelle.
 * Audit complet intégré.
 * 
 * Author: Leonel Delmat AZANGUE
 * Date: 12/10/2025
 */
@Entity
@Table(name = "contreparties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrepartie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_comptable_id", nullable = false)
    private OperationComptable operationComptable;

    @Column(length = 20, nullable = false)
    private String compte;

    @Builder.Default
    @Column(name = "est_compte_tiers", nullable = false)
    private Boolean estCompteTiers = false;

    @Pattern(regexp = "DEBIT|CREDIT", message = "Sens must be DEBIT or CREDIT")
    @Column(length = 10, nullable = false)
    private String sens;

    @Pattern(regexp = "HT|TTC|TVA|PAU", message = "Type montant must be HT, TTC, TVA, or PAU")
    @Column(name = "type_montant", length = 10, nullable = false)
    private String typeMontant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_comptable_id", nullable = false)
    private JournalComptable journalComptable;

    @Column(length = 255)
    private String notes;

    /** Dates d'audit */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Utilisateur créateur */
    @Size(max = 255)
    @Column(name = "created_by", length = 255, updatable = false)
    private String createdBy;

    /** Utilisateur ayant modifié la ressource */
    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
