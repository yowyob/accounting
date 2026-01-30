package com.yowyob.erp.accounting.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import com.yowyob.erp.common.persistence.SettablePersistable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "immobilisations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Immobilisation implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    private String code;
    private String libelle;

    @Column("date_acquisition")
    private LocalDate dateAcquisition;

    @Column("valeur_origine")
    private BigDecimal valeurOrigine;

    @Column("duree_vie")
    private Integer dureeVie;

    @Column("methode_amortissement")
    @Builder.Default
    private String methodeAmortissement = "LINEAIRE";

    @Column("compte_immo_id")
    private UUID compteImmoId;

    @Column("compte_amort_id")
    private UUID compteAmortId;

    @Column("compte_dotation_id")
    private UUID compteDotationId;

    @Builder.Default
    private String statut = "ACTIF";

    @Column("valeur_residuelle")
    @Builder.Default
    private BigDecimal valeurResiduelle = BigDecimal.ZERO;

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

    @Override
    @Transient
    public boolean isNew() {
        return isNew || id == null;
    }

    @Override
    public void setNotNew() {
        this.isNew = false;
    }
}
