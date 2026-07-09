package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(name = "charges_ventilees")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChargeVentilee implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("charge_source_id")
    private String chargeSourceId;

    @Column("compte_cg")
    private String compteCG;

    @Column("libelle")
    private String libelle;

    @Column("montant_total")
    private BigDecimal montantTotal;

    @Builder.Default
    @Column("incorporable")
    private Boolean incorporable = true;

    @Column("periode_id")
    private UUID periodeId;

    @Column("periode_cg_id")
    private UUID periodeCgId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("created_by")
    private String createdBy;

    @Column("updated_by")
    private String updatedBy;

    @Transient
    private List<VentilationCharge> ventilations;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}
