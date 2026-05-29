package com.yowyob.erp.accounting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a bank statement line (Relevé Bancaire) for R2DBC.
 */
@Table(name = "releve_bancaire")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleveBancaire implements SettablePersistable<UUID> {

    @Id
    @Column("releve_bancaire_id")
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("compte_id")
    private UUID compteId; // The bank account in the ledger

    @Column("date_operation")
    private LocalDateTime dateOperation;

    @Column("date_valeur")
    private LocalDateTime dateValeur;

    @Column("libelle")
    private String libelle;

    @Column("reference")
    private String reference; // Check number, wire ref, etc.

    @Column("montant")
    private BigDecimal montant;

    @Column("sens")
    private String sens; // "CREDIT" or "DEBIT"

    @Column("categorie")
    private String categorieDetectee;

    @Builder.Default
    @Column("rapproche")
    private boolean rapproche = false;

    @Column("date_rapprochement")
    private LocalDateTime dateRapprochement;

    @Column("detail_ecriture_id")
    private UUID detailEcritureId; // Link to the matched ledger entry detail

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