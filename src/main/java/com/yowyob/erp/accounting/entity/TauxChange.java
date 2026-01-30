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
public class TauxChange {

    @Id
    private UUID id;

    @NotNull
    @Column("tenant_id")
    private UUID tenantId;

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
    private Tenant tenant;

    @Transient
    private Devise devise_source;

    @Transient
    private Devise devise_cible;
}
