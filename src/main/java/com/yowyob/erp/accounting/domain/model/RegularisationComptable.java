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

@Table("regularisations_comptables")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegularisationComptable implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("type_regularisation")
    private TypeRegularisation typeRegularisation;

    @Builder.Default
    @Column("statut")
    private StatutRegularisation statut = StatutRegularisation.ACTIVE;

    /** Période comptable d'origine (fin de période où la régularisation est passée) */
    @Column("periode_id")
    private UUID periodeId;

    @Column("date_regularisation")
    private LocalDate dateRegularisation;

    /**
     * Compte de charge (6xx) ou produit (7xx) concerné.
     * Pour CCA/CAP : compte de charge. Pour PCA/PAR : compte de produit.
     */
    @Column("compte_charge_produit_id")
    private UUID compteChargeProduitId;

    /**
     * Compte de régularisation OHADA :
     * CCA → 476, PCA → 477, CAP → 408/428/448, PAR → 418/438
     */
    @Column("compte_regularisation_id")
    private UUID compteRegularisationId;

    @Column("montant")
    private BigDecimal montant;

    @Column("libelle")
    private String libelle;

    @Column("notes")
    private String notes;

    /** Écriture comptable initiale générée lors de la création */
    @Column("ecriture_initiale_id")
    private UUID ecritureInitialeId;

    /** Date prévue pour l'extourne (1er jour de la période suivante) */
    @Column("date_extourne")
    private LocalDate dateExtourne;

    /** Écriture d'extourne générée automatiquement */
    @Column("ecriture_extourne_id")
    private UUID ecritureExtourneId;

    @Column("extournee_par")
    private String extourneePar;

    @Column("date_extourne_effective")
    private LocalDateTime dateExtourneEffective;

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
    public boolean isNew() {
        return isNew || id == null;
    }

    @Override
    public void setNotNew() {
        this.isNew = false;
    }
}
