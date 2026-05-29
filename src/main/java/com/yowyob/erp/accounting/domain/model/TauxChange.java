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
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an Exchange Rate (Taux de Change) for R2DBC.
 */
@Table(name = "taux_change")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TauxChange implements com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable<UUID> {

    @Id
    private UUID id;

    @NotNull
    @Column("organization_id")
    private UUID organizationId;

    @NotNull
    @Column("devise_source_id")
    private UUID devise_source_id;

    @NotNull
    @Column("devise_cible_id")
    private UUID devise_cible_id;

    @NotNull
    @Column("taux")
    private BigDecimal taux;

    @NotNull
    @Column("date_effet")
    private LocalDateTime date_effet;

    @Column("notes")
    private String notes;

    @Builder.Default
    @Column("created_at")
    private LocalDateTime created_at = LocalDateTime.now();

    @Transient
    private Organization organization;

    @Transient
    private Devise devise_source;

    @Transient
    private Devise devise_cible;

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
