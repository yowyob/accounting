package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import java.math.BigDecimal;
import java.util.UUID;

@Table(name = "cles_repartition_lignes")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CleRepartitionLigne implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("cle_id")
    private UUID cleId;

    @Column("centre_destinataire_id")
    private UUID centreDestinataireId;

    @Column("pourcentage")
    private BigDecimal pourcentage; // Pour les clés fixes (ex: 20.00 %)

    @Column("unite_oeuvre_id")
    private UUID uniteOeuvreId; // Pour les clés basées sur l'activité réelle d'un centre auxiliaire

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}
