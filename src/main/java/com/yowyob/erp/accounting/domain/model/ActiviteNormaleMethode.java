package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table(name = "activites_normales_methode")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ActiviteNormaleMethode {

    @Id
    private UUID id;

    @Column("methode_calcul_id")
    private UUID methodeCalculId;

    @Column("centre_id")
    private UUID centreId;

    @Column("centre_libelle")
    private String centreLibelle;

    @Column("activite_normale")
    private BigDecimal activiteNormale;

    @Column("unite")
    private String unite;
}
