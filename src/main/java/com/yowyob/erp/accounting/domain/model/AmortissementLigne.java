package com.yowyob.erp.accounting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Table(name = "amortissements_lignes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmortissementLigne {

    @Id
    private UUID id;

    @Column("immo_id")
    private UUID immoId;

    @Column("exercice_id")
    private UUID exerciceId;

    @Column("date_echeance")
    private LocalDate dateEcheance;

    @Column("base_calcul")
    private BigDecimal baseCalcul;

    private BigDecimal taux;
    private BigDecimal annuite;

    @Column("cumul_amortissement")
    private BigDecimal cumulAmortissement;

    @Column("valeur_nette_comptable")
    private BigDecimal valeurNetteComptable;

    @Builder.Default
    private boolean comptabilisee = false;

    @Column("ecriture_id")
    private UUID ecritureId;
}
