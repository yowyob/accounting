package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Journal Comptable (OHADA)
 * Gère les journaux de ventes, achats, trésorerie, etc.
 */
@Entity
@Table(name = "journal_comptable")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalComptable implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "journal_comptable_id")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "code_journal", length = 20)
    private String codeJournal;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String libelle;

    @NotBlank
    @Column(name = "type_journal", length = 50, nullable = false)
    private String typeJournal;

    @Column(length = 255)
    private String notes;

    @NotNull
    @Column(nullable = false)
    private Boolean actif = true;

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