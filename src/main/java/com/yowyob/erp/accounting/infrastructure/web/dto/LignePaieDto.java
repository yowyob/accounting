package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LignePaieDto {

    private UUID id;

    @NotNull(message = "L'exercice est obligatoire")
    private UUID exerciceId;

    private UUID periodeId;

    @NotNull(message = "Le mois de paie est obligatoire (ex: 2025-03-01)")
    private LocalDate moisPaie;

    private String libelle;

    @NotNull(message = "Le salaire brut total est obligatoire")
    @Positive
    private BigDecimal salaireBrutTotal;

    @Builder.Default
    private BigDecimal retenueCnpsSalarie = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal retenueIrpp = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal autresRetenues = BigDecimal.ZERO;

    /** Calculé automatiquement si null : brut - retenues */
    private BigDecimal salaireNetTotal;

    @Builder.Default
    private BigDecimal chargePatronaleCnps = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal autresChargesPatronales = BigDecimal.ZERO;

    /** BROUILLON | VALIDE | COMPTABILISE */
    @Builder.Default
    private String statut = "BROUILLON";

    private UUID ecritureId;
    private LocalDateTime createdAt;
    private String createdBy;
}
