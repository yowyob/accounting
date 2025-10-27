package com.yowyob.erp.accounting.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Journal d’audit : trace les actions de création, validation et modification
 * Audit intégré directement dans l'entité sans utiliser d'interface.
 * 
 * Author: Leonel Delmat AZANGUE
 * Date: 12/10/2025
 */
@Entity
@Table(name = "journal_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "journal_audit_id")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "ecriture_id")
    private UUID ecritureComptableId;

    @Pattern(regexp = "CREATE|VALIDATE|UPDATE|DELETE|AUTO_GENERATE", message = "Action must be CREATE, VALIDATE,DELETE or UPDATE")
    @Column(length = 50, nullable = false)
    private String action;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction;

    @Column(length = 255)
    private String utilisateur;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "adresse_ip", length = 50)
    private String adresseIp;

    @Column(name = "donnees_avant", columnDefinition = "TEXT")
    private String donneesAvant;

    @Column(name = "donnees_apres", columnDefinition = "TEXT")
    private String donneesApres;

    /** Date de création */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Date de dernière mise à jour */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Utilisateur créateur */
    @Size(max = 255)
    @Column(name = "created_by", length = 255)
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
        if (this.dateAction == null) {
            this.dateAction = now;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
