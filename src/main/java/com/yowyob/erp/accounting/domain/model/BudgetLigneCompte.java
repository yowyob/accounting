package com.yowyob.erp.accounting.domain.model;

import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ligne budgétaire détaillée par compte comptable pour un budget de type ANALYTIQUE.
 * Permet de ventiler l'enveloppe globale d'un budget analytique sur des comptes spécifiques.
 */
@Table(name = "budget_lignes_comptes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetLigneCompte implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("budget_id")
    private UUID budgetId;

    @Column("compte_id")
    private UUID compteId;

    @Column("montant_alloue")
    private BigDecimal montantAlloue;

    @Column("description")
    private String description;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}
