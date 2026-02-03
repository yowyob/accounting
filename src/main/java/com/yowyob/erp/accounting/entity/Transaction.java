package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.persistence.SettablePersistable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * Entity representing an accounting transaction (payment, collection, or cash
 * operation) for R2DBC.
 */
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("numero_recu")
    private String numero_recu;

    @Column("operation_comptable_id")
    private UUID operation_comptable_id;

    @PositiveOrZero
    @NotNull
    @Column("montant_transaction")
    private BigDecimal montant_transaction;

    @Size(max = 255)
    @Column("montant_lettre")
    private String montant_lettre;

    @NotNull
    @Builder.Default
    @Column("est_montant_ttc")
    private Boolean est_montant_ttc = true;

    @NotNull
    @Column("date_transaction")
    private LocalDateTime date_transaction;

    @Builder.Default
    @Column("est_validee")
    private Boolean est_validee = false;

    @Column("date_validation")
    private LocalDateTime date_validation;

    @Column("reference_objet")
    private String reference_objet;

    @Column("caissier")
    private String caissier;

    @Builder.Default
    @Column("est_comptabilisee")
    private Boolean est_comptabilisee = false;

    @Column("ecriture_comptable_id")
    private UUID ecriture_comptable_id;

    @Column("notes")
    private String notes;

    @Builder.Default
    @Column("created_at")
    private LocalDateTime created_at = LocalDateTime.now();

    @Builder.Default
    @Column("updated_at")
    private LocalDateTime updated_at = LocalDateTime.now();

    @Column("created_by")
    private String created_by;

    @Column("updated_by")
    private String updated_by;

    @Transient
    private Tenant tenant;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew || id == null;
    }

    @Override
    public void setNotNew() {
        this.isNew = false;
    }
}
