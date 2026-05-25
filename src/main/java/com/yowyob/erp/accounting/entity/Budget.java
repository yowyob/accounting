package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.persistence.SettablePersistable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Budget hiérarchique : EXERCICE → PERIODE → ANALYTIQUE.
 * - EXERCICE  : enveloppe annuelle globale (lié à exercice_id, pas de parent).
 * - PERIODE   : découpage mensuel/trimestriel (parent = EXERCICE, lié à periode_id).
 * - ANALYTIQUE: ventilation par axe analytique et comptes (parent = PERIODE ou EXERCICE).
 */
@Table(name = "budgets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Budget implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("exercice_id")
    private UUID exerciceId;

    @Column("periode_id")
    private UUID periodeId;

    /** FK récursive vers le budget parent (null pour les budgets EXERCICE) */
    @Column("parent_id")
    private UUID parentId;

    /** Compte comptable (utilisé pour les lignes de type PREVISIONNEL ancien style) */
    @Column("compte_id")
    private UUID compteId;

    @Column("code")
    private String code;

    @Column("nom")
    private String nom;

    @Column("montant_alloue")
    private BigDecimal montantAlloue;

    @Column("libelle")
    private String libelle;

    @Column("notes")
    private String notes;

    /**
     * Type de nœud budgétaire : EXERCICE | PERIODE | ANALYTIQUE | PREVISIONNEL | REVISE
     */
    @Builder.Default
    @Column("type")
    private String type = "EXERCICE";

    /**
     * Statut du workflow : BROUILLON | VALIDE | ACTIF | INACTIF | CLOTURE
     */
    @Builder.Default
    @Column("statut")
    private String statut = "BROUILLON";

    /** Seuil d'alerte en % (déclenche une notification quand montantConsomme/montantAlloue >= seuilAlerte/100) */
    @Builder.Default
    @Column("seuil_alerte")
    private Integer seuilAlerte = 80;

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

    @Override
    @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}
