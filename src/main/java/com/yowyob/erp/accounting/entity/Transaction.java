package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * Entity representing an accounting transaction (payment, collection, or cash
 * operation).
 * Follows snake_case naming as per Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "numero_recu", length = 100)
    private String numero_recu;

    @Column(name = "operation_comptable_id")
    private UUID operation_comptable_id;

    @PositiveOrZero
    @NotNull
    @Column(name = "montant_transaction", nullable = false, precision = 18, scale = 2)
    private BigDecimal montant_transaction;

    @Size(max = 255)
    @Column(name = "montant_lettre")
    private String montant_lettre;

    @NotNull
    @Builder.Default
    @Column(name = "est_montant_ttc", nullable = false)
    private Boolean est_montant_ttc = true;

    @NotNull
    @Column(name = "date_transaction", nullable = false)
    private LocalDateTime date_transaction;

    @Builder.Default
    @Column(name = "est_validee", nullable = false)
    private Boolean est_validee = false;

    @Column(name = "date_validation")
    private LocalDateTime date_validation;

    @Column(name = "reference_objet", length = 255)
    private String reference_objet;

    @Column(name = "caissier", length = 255)
    private String caissier;

    @Builder.Default
    @Column(name = "est_comptabilisee", nullable = false)
    private Boolean est_comptabilisee = false;

    @Column(name = "ecriture_comptable_id")
    private UUID ecriture_comptable_id;

    @Column(length = 500)
    private String notes;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime created_at = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updated_at = LocalDateTime.now();

    @Column(name = "created_by", length = 50)
    private String created_by;

    @Column(name = "updated_by", length = 50)
    private String updated_by;

    @PrePersist
    public void onCreate() {
        this.created_at = LocalDateTime.now();
        this.updated_at = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updated_at = LocalDateTime.now();
    }
}
