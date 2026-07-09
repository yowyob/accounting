package com.yowyob.erp.accounting.domain.model;

import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "configuration_analytique")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ConfigurationAnalytique implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Builder.Default
    @Column("devise")
    private String devise = "FCFA";

    @Builder.Default
    @Column("precision")
    private Integer precision = 0;

    @Builder.Default
    @Column("separateur_milliers")
    private String separateurMilliers = " ";

    @Builder.Default
    @Column("bloquer_apres_cloture_cg")
    private Boolean bloquerApresClotureCg = true;

    @Builder.Default
    @Column("jours_grace_cloture")
    private Integer joursGraceCloture = 5;

    @Builder.Default
    @Column("autoriser_saisie_retroactive")
    private Boolean autoriserSaisieRetroactive = false;

    @Builder.Default
    @Column("methode_valorisation_stocks")
    private String methodeValorisationStocks = "CUMP";

    @Builder.Default
    @Column("import_comptabilite_generale_active")
    private Boolean importComptabiliteGeneraleActive = false;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("created_by")
    private String createdBy;

    @Column("updated_by")
    private String updatedBy;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}
