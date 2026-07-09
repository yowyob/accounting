package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table(name = "lignes_cout_standard")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LigneCoutStandard {

    @Id
    private UUID id;

    @Column("fiche_id")
    private UUID ficheId;

    @Column("composante")
    private String composante;

    @Column("centre_id")
    private UUID centreId;

    @Column("libelle")
    private String libelle;

    @Column("quantite_standard")
    private BigDecimal quantiteStandard;

    @Column("cout_unitaire_standard")
    private BigDecimal coutUnitaireStandard;

    @Column("cout_standard_total")
    private BigDecimal coutStandardTotal;

    @Column("activite_normale")
    private BigDecimal activiteNormale;
}
