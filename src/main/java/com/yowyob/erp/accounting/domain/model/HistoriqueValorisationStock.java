package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Table(name = "historique_valorisation_stock")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HistoriqueValorisationStock {

    @Id
    private UUID id;

    @Column("regle_id")
    private UUID regleId;

    @Column("methode")
    private String methode;

    @Column("date_du")
    private LocalDate dateDu;

    @Column("date_au")
    private LocalDate dateAu;
}
