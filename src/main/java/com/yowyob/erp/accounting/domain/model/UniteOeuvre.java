package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "unites_oeuvre")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UniteOeuvre implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("code")
    private String code;

    @Column("libelle")
    private String libelle;

    @Column("unite")
    private String unite; // HEURE_MACHINE, KG, KWH, M2, HEURE_MOD, etc.

    @Column("centre_id")
    private UUID centreId;

    @Column("cout_unitaire_previsionnel")
    private BigDecimal coutUnitairePrevisionnel;

    @Builder.Default
    @Column("actif")
    private Boolean actif = true;

    @Column("created_by")
    private String createdBy;

    @Column("updated_by")
    private String updatedBy;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}
