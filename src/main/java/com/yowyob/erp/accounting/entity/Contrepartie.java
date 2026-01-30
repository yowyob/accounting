package com.yowyob.erp.accounting.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a counterparty for an accounting operation for R2DBC.
 */
@Table(name = "contreparties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrepartie {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("operation_comptable_id")
    private UUID operation_comptable_id;

    @Column("compte_id")
    private UUID compte_id;

    @Builder.Default
    @Column("est_compte_tiers")
    private Boolean est_compte_tiers = false;

    @Pattern(regexp = "DEBIT|CREDIT", message = "Direction must be DEBIT or CREDIT")
    @Column("sens")
    private String sens;

    @Pattern(regexp = "HT|TTC|TVA|PAU", message = "Type montant must be HT, TTC, TVA, or PAU")
    @Column("type_montant")
    private String type_montant;

    @Column("journal_comptable_id")
    private UUID journal_comptable_id;

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
    private Tenant tenant;

    @Transient
    private OperationComptable operation_comptable;

    @Transient
    private JournalComptable journal_comptable;
}
