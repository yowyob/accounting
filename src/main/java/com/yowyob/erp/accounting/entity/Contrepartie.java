package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contrepartie d'une opération comptable (Phase 2 du paramétrage)
 */
@Entity
@Table(name = "contrepartie")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrepartie implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contrepartie_id")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "operation_comptable_id")
    private Long operationComptableId;

    @Column(length = 20)
    private String compte;

    @Column(name = "est_compte_tiers")
    private Boolean estCompteTiers = false;

    @Pattern(regexp = "DEBIT|CREDIT")
    @Column(length = 10)
    private String sens;

    @Pattern(regexp = "HT|TTC|TVA|PAU")
    @Column(name = "type_montant", length = 10)
    private String typeMontant;

    @Column(name = "journal_comptable_id")
    private Long journalComptableId;

 
    @Column(length = 255)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

             /** Utilisateur créateur */
    @Size(max = 255)
    @Column(name = "created_by", length = 255)
    private String createdBy;

    /** Utilisateur ayant modifié la ressource */
    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Override
    public UUID getTenantId() { return tenantId; }

    @Override
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
}
