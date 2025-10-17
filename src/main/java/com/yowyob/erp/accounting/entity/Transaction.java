package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * Transaction comptable : paiement, encaissement ou opération de caisse.
 */
@Entity
@Table(name = "transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "numero_recu", length = 100)
    private String numeroRecu;

    @Column(name = "operation_comptable_id")
    private UUID operationComptableId;

    @PositiveOrZero
    @NotNull
    @Column(name = "montant_transaction", nullable = false)
    private BigDecimal montantTransaction;

    @Size(max = 255)
    @Column(name = "montant_lettre")
    private String montantLettre;

    @NotNull
    @Builder.Default
    @Column(name = "est_montant_ttc", nullable = false)
    private Boolean estMontantTTC = true;

    @NotNull
    @Column(name = "date_transaction", nullable = false)
    private LocalDateTime dateTransaction;

    @Builder.Default
    @Column(name = "est_validee", nullable = false)
    private Boolean estValidee = false;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "reference_objet", length = 255)
    private String referenceObjet;

    @Column(name = "caissier", length = 255)
    private String caissier;

    @Builder.Default
    @Column(name = "est_comptabilisee", nullable = false)
    private Boolean estComptabilisee = false;

    @Column(name = "ecriture_comptable_id")
    private UUID ecritureComptableId;

    @Column(length = 255)
    private String notes;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;


}
