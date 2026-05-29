package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmortissementLigneDto {
    private UUID id;
    private UUID immoId;
    private LocalDate dateEcheance;
    private BigDecimal baseCalcul;
    private BigDecimal taux;
    private BigDecimal annuite;
    private BigDecimal cumulAmortissement;
    private BigDecimal valeurNetteComptable;
    private boolean comptabilisee;
    private UUID ecritureId;
}
