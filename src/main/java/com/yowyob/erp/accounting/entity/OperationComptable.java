package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Opération comptable paramétrable (Phase 1 du paramétrage OHADA)
 */
@Entity
@Table(name = "operation_comptable")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationComptable implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operation_id")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotBlank
    @Column(name = "type_operation", length = 50)
    private String typeOperation;

    @NotBlank
    @Column(name = "mode_reglement", length = 50)
    private String modeReglement;

    @NotBlank
    @Column(name = "compte_principal", length = 20)
    private String comptePrincipal;

    @Column(name = "est_compte_statique")
    private Boolean estCompteStatique = false;

    @Pattern(regexp = "DEBIT|CREDIT")
    @Column(name = "sens_principal", length = 10)
    private String sensPrincipal;

    @Column(name = "journal_comptable_id")
    private Long journalComptableId;

    @Pattern(regexp = "HT|TTC|TVA|PAU")
    @Column(name = "type_montant", length = 10)
    private String typeMontant;

    @Column(name = "plafond_client")
    private Double plafondClient = 0.0;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(length = 255)
    private String notes;

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
