package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChargeAnalytiqueDto {
    private UUID id;

    @NotNull(message = "La nature est obligatoire")
    private String nature;

    @NotNull(message = "Le montant est obligatoire")
    private BigDecimal montant;

    @NotNull(message = "Le type est obligatoire")
    private String type;

    @Builder.Default
    private Boolean incorporable = true;

    @NotNull(message = "Le centre est obligatoire")
    private UUID centreId;

    private String centreLibelle;

    @NotNull(message = "La période est obligatoire")
    private UUID periodeId;

    private String description;
}
