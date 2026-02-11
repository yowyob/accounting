package com.yowyob.erp.accounting.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.yowyob.erp.common.persistence.SettablePersistable;

/**
 * Entity representing an accounting entry (Ecriture Comptable) for R2DBC.
 */
@Table(name = "ecritures_comptables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcritureComptable implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("journal_id")
    private UUID journal_id;

    @Column("periode_id")
    private UUID periode_id;

    @Column("numero_ecriture")
    private String numero_ecriture;

    @Column("libelle")
    private String libelle;

    @Column("reference_externe")
    private String reference_externe;

    @Column("notes")
    private String notes;

    @Column("attachment_ids")
    private JsonNode attachment_ids;

    @Column("date_ecriture")
    private LocalDate date_ecriture;

    @Column("montant_total_debit")
    private BigDecimal montant_total_debit;

    @Column("montant_total_credit")
    private BigDecimal montant_total_credit;

    @Builder.Default
    @Column("validee")
    private Boolean validee = false;

    @Builder.Default
    @Column("statut")
    private EcritureStatut statut = EcritureStatut.BROUILLON;

    @Builder.Default
    @Column("actif")
    private Boolean actif = true;

    @Column("date_validation")
    private LocalDateTime date_validation;

    @Transient
    private List<DetailEcriture> details;

    @Column("created_at")
    private LocalDateTime created_at;

    @Column("updated_at")
    private LocalDateTime updated_at;

    @Size(max = 255)
    @Column("created_by")
    private String created_by;

    @Size(max = 255)
    @Column("updated_by")
    private String updated_by;

    @Size(max = 255)
    @Column("validated_by")
    private String validated_by;

    @Transient
    private Organization organization;

    @Transient
    private JournalComptable journal;

    @Transient
    private PeriodeComptable periode;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }

    public void setNotNew() {
        this.isNew = false;
    }
}
