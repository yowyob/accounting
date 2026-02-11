package com.yowyob.erp.accounting.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.common.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an accounting account (Compte) for R2DBC.
 * Follows snake_case naming as per Development Charter.
 */
@Table(name = "comptes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compte implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("external_id")
    private UUID external_id;

    @Column("no_compte")
    private String no_compte;

    @Column("libelle")
    private String libelle;

    @Column("notes")
    private String notes;

    @Builder.Default
    @Column("solde")
    private BigDecimal solde = BigDecimal.ZERO;

    @Column("classe")
    private Integer classe; // OHADA class (1 to 7)

    @Column("type_compte")
    private String type_compte; // ACTIF, PASSIF, CHARGE, PRODUIT

    @Builder.Default
    @Column("actif")
    private Boolean actif = true;

    @Column("created_by")
    private String created_by;

    @Column("updated_by")
    private String updated_by;

    @Column("created_at")
    private LocalDateTime created_at;

    @Column("updated_at")
    private LocalDateTime updated_at;

    @Transient
    private Organization organization; // Kept as transient for DTO mapping if needed

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew || id == null;
    }

    public void setNotNew() {
        this.isNew = false;
    }

    public void updateSolde(BigDecimal new_solde) {
        this.solde = new_solde;
    }
}
