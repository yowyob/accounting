package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Déclaration fiscale : TVA, IS, etc.
 */
@Entity
@Table(name = "declaration_fiscale")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclarationFiscale implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "declaration_id")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotBlank
    @Column(name = "type_declaration", length = 50)
    private String typeDeclaration;

    @NotNull
    @Column(name = "periode_debut")
    private LocalDate periodeDebut;

    @NotNull
    @Column(name = "periode_fin")
    private LocalDate periodeFin;

    @PositiveOrZero
    @Column(name = "montant_total")
    private Double montantTotal = 0.0;

    @Column(name = "date_generation")
    private LocalDate dateGeneration;

    @Pattern(regexp = "DRAFT|SUBMITTED|VALIDATED")
    @Column(length = 50)
    private String statut;

    @Column(name = "numero_declaration", length = 100)
    private String numeroDeclaration;

    @Column(name = "donnees_declaration", columnDefinition = "TEXT")
    private String donneesDeclaration;

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
