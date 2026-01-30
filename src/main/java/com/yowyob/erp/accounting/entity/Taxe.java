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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a tax (Taxe) for R2DBC.
 */
@Table(name = "taxes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Taxe {

    @Id
    private UUID id;

    @NotNull
    @Column("tenant_id")
    private UUID tenantId;

    @NotBlank
    @Column("code")
    private String code;

    @NotBlank
    @Column("libelle")
    private String libelle;

    @NotNull
    @Positive
    @Column("taux")
    private BigDecimal taux;

    @Column("type_taxe")
    private String type_taxe;

    @Column("compte_collecte")
    private String compte_collecte;

    @Column("compte_deductible")
    private String compte_deductible;

    @Column("pays")
    private String pays;

    @Column("date_debut_validite")
    private LocalDate date_debut_validite;

    @Column("date_fin_validite")
    private LocalDate date_fin_validite;

    @Builder.Default
    @Column("actif")
    private boolean actif = true;

    @Builder.Default
    @Column("created_at")
    private LocalDateTime created_at = LocalDateTime.now();

    @Transient
    private Tenant tenant;
}