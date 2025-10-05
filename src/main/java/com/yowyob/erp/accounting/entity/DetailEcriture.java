package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Détail d’une écriture comptable (ligne débit ou crédit)
 */
@Entity
@Table(name = "detail_ecriture")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailEcriture implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_ecriture_id")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "ecriture_id")
    private Long ecritureComptableId;

    @Column(name = "plan_comptable_id")
    private Long compteComptableId;

    @Column(length = 255)
    private String libelle;

    @Pattern(regexp = "DEBIT|CREDIT")
    @Column(length = 10)
    private String sens;

    @Column(name = "montant_debit")
    private Double montantDebit = 0.0;

    @Column(name = "montant_credit")
    private Double montantCredit = 0.0;

    @Column(length = 255)
    private String notes;

    @Column(name = "date_ecriture")
    private LocalDateTime dateEcriture;

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
