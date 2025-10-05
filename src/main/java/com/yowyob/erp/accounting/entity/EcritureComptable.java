package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import com.yowyob.erp.common.enums.SourceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ecriture comptable : représente une opération comptable
 */
@Entity
@Table(name = "ecriture_comptable")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcritureComptable implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ecriture_id")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "numero_ecriture", length = 100, unique = true)
    private String numeroEcriture;

    @Column(nullable = false, length = 255)
    private String libelle;

    @NotNull
    @Column(name = "date_ecriture", nullable = false)
    private LocalDate dateEcriture;

    @Column(name = "journal_comptable_id")
    private Long journalComptableId;

    @Column(name = "periode_comptable_id")
    private Long periodeComptableId;

    @Column(name = "montant_total_debit")
    private Double montantTotalDebit = 0.0;

    @Column(name = "montant_total_credit")
    private Double montantTotalCredit = 0.0;

    @Column(nullable = false)
    private Boolean validee = false;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "utilisateur_validation")
    private String utilisateurValidation;

    @Column(name = "reference_externe")
    private String referenceExterne;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private SourceType sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

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