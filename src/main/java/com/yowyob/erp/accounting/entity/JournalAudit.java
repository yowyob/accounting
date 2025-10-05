package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Journal d’audit : trace les actions de création, validation et modification
 */
@Entity
@Table(name = "journal_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalAudit implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "journal_audit_id")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "ecriture_id")
    private Long ecritureComptableId;

    @Pattern(regexp = "CREATION|VALIDATION|MODIFICATION")
    @Column(length = 50)
    private String action;

    @Column(name = "date_action")
    private LocalDateTime dateAction = LocalDateTime.now();

    @Column(length = 255)
    private String utilisateur;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "adresse_ip")
    private String adresseIp;

    @Column(name = "donnees_avant", columnDefinition = "TEXT")
    private String donneesAvant;

    @Column(name = "donnees_apres", columnDefinition = "TEXT")
    private String donneesApres;

    /**
     * Date de création
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Date de dernière mise à jour
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Utilisateur créateur
     */
    @Size(max = 255)
    @Column(name = "created_by", length = 255)
    private String createdBy;

    /**
     * Utilisateur ayant modifié la ressource
     */
    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    // =========================================================
    // Implémentation de l'interface Auditable
    // =========================================================
    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
