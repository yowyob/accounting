package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction comptable : paiement, encaissement ou opération de caisse.
 */
@Entity
@Table(name = "transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "numero_recu", length = 100)
    private String numeroRecu;

    @Column(name = "operation_comptable_id")
    private Long operationComptableId;

    @PositiveOrZero
    @NotNull
    @Column(name = "montant_transaction", nullable = false)
    private Double montantTransaction;

    @Size(max = 255)
    @Column(name = "montant_lettre")
    private String montantLettre;

    @NotNull
    @Column(name = "est_montant_ttc", nullable = false)
    private Boolean estMontantTTC = true;

    @NotNull
    @Column(name = "date_transaction", nullable = false)
    private LocalDateTime dateTransaction;

    @Column(name = "est_validee", nullable = false)
    private Boolean estValidee = false;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "reference_objet", length = 255)
    private String referenceObjet;

    @Column(name = "caissier", length = 255)
    private String caissier;

    @Column(name = "est_comptabilisee", nullable = false)
    private Boolean estComptabilisee = false;

    @Column(name = "ecriture_comptable_id")
    private Long ecritureComptableId;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Override
    public UUID getTenantId() { return tenantId; }

    @Override
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
}
