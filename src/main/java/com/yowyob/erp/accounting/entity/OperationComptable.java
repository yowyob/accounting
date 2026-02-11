package com.yowyob.erp.accounting.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import com.yowyob.erp.common.persistence.SettablePersistable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Parametrable accounting operation (Phase 1 of OHADA configuration) for R2DBC.
 */
@Table(name = "operation_comptable")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationComptable implements SettablePersistable<UUID> {

    @Id
    @Column("operation_id")
    private UUID id;

    @NotNull
    @Column("organization_id")
    private UUID organizationId;

    @NotBlank
    @Column("type_operation")
    private String type_operation;

    @NotBlank
    @Column("mode_reglement")
    private String mode_reglement;

    @NotNull
    @Column("compte_principal_id")
    private UUID compte_principal_id;

    @Builder.Default
    @Column("est_compte_statique")
    private Boolean est_compte_statique = false;

    @Pattern(regexp = "DEBIT|CREDIT", message = "Sens principal must be DEBIT or CREDIT")
    @Column("sens_principal")
    private String sens_principal;

    @Column("journal_comptable_id")
    private UUID journal_comptable_id;

    @Pattern(regexp = "HT|TTC|TVA|PAU", message = "Type montant must be HT, TTC, TVA, or PAU")
    @Column("type_montant")
    private String type_montant;

    @PositiveOrZero
    @Builder.Default
    @Column("plafond_client")
    private BigDecimal plafond_client = BigDecimal.ZERO;

    @Builder.Default
    @Column("actif")
    private Boolean actif = true;

    @Column("notes")
    private String notes;

    @Column("created_at")
    private LocalDateTime created_at;

    @Column("updated_at")
    private LocalDateTime updated_at;

    @Size(max = 255)
    @Column("created_by")
    private String created_by;

    @Size(max = 255)
    @Column("updated_by")
    private String updated_by;

    @Transient
    private Organization tenant;

    @Transient
    private JournalComptable journal_comptable;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew || id == null;
    }

    public void setNotNew() {
        this.isNew = false;
    }
}