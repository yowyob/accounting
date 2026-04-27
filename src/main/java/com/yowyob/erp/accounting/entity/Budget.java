package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.persistence.SettablePersistable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ligne budgétaire : montant prévu pour un compte sur une période donnée.
 * Permet la comparaison budget vs réalisé.
 */
@Table(name = "budgets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Budget implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("exercice_id")
    private UUID exerciceId;

    @Column("periode_id")
    private UUID periodeId;

    @Column("compte_id")
    private UUID compteId;

    @Column("montant_budget")
    private BigDecimal montantBudget;

    private String libelle;
    private String notes;

    /** PREVISIONNEL | REVISE */
    @Builder.Default
    private String type = "PREVISIONNEL";

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("created_by")
    private String createdBy;

    @Column("updated_by")
    private String updatedBy;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}
