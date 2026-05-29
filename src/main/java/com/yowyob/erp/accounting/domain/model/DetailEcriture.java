package com.yowyob.erp.accounting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.yowyob.erp.shared.domain.enums.Sens;

import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;

/**
 * Entity representing an accounting entry detail for R2DBC.
 * Contains debit or credit amounts for a specific account within an entry.
 */
@Table(name = "details_ecritures")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailEcriture implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("ecriture_id")
    private UUID ecriture_id;

    @Column("compte_id")
    private UUID compte_id;

    @Column("libelle")
    private String libelle;

    @Column("notes")
    private String notes;

    @Column("sens")
    private Sens sens;

    @Column("montant_debit")
    private BigDecimal montant_debit;

    @Column("montant_credit")
    private BigDecimal montant_credit;

    @Column("lettree")
    @Builder.Default
    private Boolean lettree = false;

    @Column("date_lettrage")
    private LocalDateTime date_lettrage;

    @Column("pointee")
    @Builder.Default
    private Boolean pointee = false;

    @Column("reference_bancaire")
    private String reference_bancaire;

    @Column("date_ecriture")
    private LocalDateTime date_ecriture;

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

    @Transient
    private Organization organization;

    @Transient
    private EcritureComptable ecriture;

    @Transient
    private Compte compte;

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
