package com.yowyob.erp.accounting.domain.model;

import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "regles_incorporation")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RegleIncorporation implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("compte_cg_id")
    private UUID compteCgId;

    @Column("compte_cg_no")
    private String compteCgNo;

    @Column("libelle")
    private String libelle;

    @Column("mode")
    private String mode;

    @Column("taux_substitution")
    private BigDecimal tauxSubstitution;

    @Column("montant_substitution")
    private BigDecimal montantSubstitution;

    @Column("base_calcul")
    private String baseCalcul;

    @Column("justification")
    private String justification;

    @Column("compte_ecart97")
    private String compteEcart97;

    @Column("periode_id")
    private UUID periodeId;

    @Column("date_debut")
    private LocalDate dateDebut;

    @Column("date_fin")
    private LocalDate dateFin;

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
