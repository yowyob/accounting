package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import java.math.BigDecimal;
import java.util.UUID;

@Table(name = "lignes_imputation")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LigneImputation implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("ecriture_id")
    private UUID ecritureId;

    @Column("centre_id")
    private UUID centreId;

    @Column("unite_oeuvre_id")
    private UUID uniteOeuvreId;

    @Column("montant")
    private BigDecimal montant;

    @Column("quantite_uo")
    private BigDecimal quantiteUo;

    @Builder.Default
    @Column("sens")
    private String sens = "DEBIT"; // DEBIT, CREDIT

    @Column("libelle")
    private String libelle;

    @Column("cle_repartition_id")
    private UUID cleRepartitionId;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}
