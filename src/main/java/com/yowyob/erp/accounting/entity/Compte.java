package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.yowyob.erp.common.entity.Auditable;

/**
 * Entité représentant un compte comptable du Plan Comptable OHADA.
 *
 */
@Entity
@Table(name = "account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compte implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    /**
     * Identifiant du locataire (multi-tenant isolation)
     */
    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Numéro de compte OHADA (ex: 101000, 445700)
     */
    @NotBlank(message = "Le numéro de compte ne peut pas être vide")
    @Size(max = 20, message = "Le numéro de compte ne doit pas dépasser 20 caractères")
    @Column(name = "no_compte", unique = true, nullable = false, length = 20)
    private String noCompte;

    /**
     * Libellé du compte
     */
    @NotBlank(message = "Le libellé ne peut pas être vide")
    @Size(max = 255, message = "Le libellé ne doit pas dépasser 255 caractères")
    @Column(name = "libelle", nullable = false, length = 255)
    private String libelle;

    /**
     * Notes ou commentaires
     */
    @Size(max = 255)
    @Column(name = "notes")
    private String notes;

    /**
     * Solde courant du compte
     */
    @Column(name = "soldes", precision = 18, scale = 2)
    private BigDecimal soldes = BigDecimal.ZERO;

    /**
     * Classe OHADA (1 à 7)
     */
    @NotNull(message = "La classe ne peut pas être nulle")
    @Column(name = "classe", nullable = false)
    private Integer classe;

    /**
     * Type de compte : ACTIF / PASSIF / PRODUIT / CHARGE
     */
    @NotNull(message = "Le type de compte ne peut pas être nul")
    @Size(max = 50)
    @Column(name = "type_compte", nullable = false, length = 50)
    private String typeCompte;

    /**
     * Statut actif/inactif
     */
    @NotNull
    @Column(name = "actif", nullable = false)
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
