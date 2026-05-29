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

/**
 * Bulletin de paie agrégé d'un mois donné.
 * Génère automatiquement les écritures comptables OHADA :
 *   6611 Traitements et salaires (Débit salaire brut)
 *   4311 CNPS (Crédit retenue salariale)
 *   4441 IRPP à reverser (Crédit retenue IRPP)
 *   4220 Rémunérations dues au personnel (Crédit salaire net)
 *   6613 Charges patronales CNPS (Débit charge patronale)
 *   4312 CNPS part patronale (Crédit)
 */
@Table(name = "lignes_paie")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LignePaie implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("exercice_id")
    private UUID exerciceId;

    @Column("periode_id")
    private UUID periodeId;

    @Column("mois_paie")
    private LocalDate moisPaie;

    private String libelle;

    // ─── Montants bruts ───
    @Column("salaire_brut_total")
    private BigDecimal salaireBrutTotal;

    // ─── Retenues salariales ───
    @Column("retenue_cnps_salarie")
    private BigDecimal retenueCnpsSalarie;

    @Column("retenue_irpp")
    private BigDecimal retenueIrpp;

    @Column("autres_retenues")
    private BigDecimal autresRetenues;

    @Column("salaire_net_total")
    private BigDecimal salaireNetTotal;

    // ─── Charges patronales ───
    @Column("charge_patronale_cnps")
    private BigDecimal chargePatronaleCnps;

    @Column("autres_charges_patronales")
    private BigDecimal autresChargesPatronales;

    // ─── Statut ───
    /** BROUILLON | VALIDE | COMPTABILISE */
    @Builder.Default
    private String statut = "BROUILLON";

    @Column("ecriture_id")
    private UUID ecritureId;

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
