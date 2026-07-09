package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.util.UUID;

@Table(name = "ventilations_charge")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class VentilationCharge implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("charge_ventilee_id")
    private UUID chargeVentileeId;

    @Column("axe_id")
    private UUID axeId;

    @Column("centre_id")
    private UUID centreId;

    @Column("pourcentage")
    private BigDecimal pourcentage;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}
