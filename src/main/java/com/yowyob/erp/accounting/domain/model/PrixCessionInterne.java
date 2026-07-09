package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "prix_cessions_internes")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PrixCessionInterne implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("centre_cedant_id")
    private UUID centreCedantId;

    @Column("centre_beneficiaire_id")
    private UUID centreBeneficiaireId;

    @Column("prestation_libelle")
    private String prestationLibelle;

    @Column("methode")
    private String methode;

    @Column("prix_unitaire")
    private BigDecimal prixUnitaire;

    @Column("unite_id")
    private UUID uniteId;

    @Column("date_debut")
    private LocalDate dateDebut;

    @Column("date_fin")
    private LocalDate dateFin;

    @Builder.Default
    @Column("has_imputations")
    private Boolean hasImputations = false;

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
