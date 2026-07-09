package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UniteOeuvreDto {
    private UUID id;

    @NotNull(message = "Le code est obligatoire")
    private String code;

    @NotNull(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "L'unité est obligatoire")
    private String unite; // HEURE_MACHINE, KG, KWH, M2, HEURE_MOD

    private UUID centreId;
    private String centreLibelle;
    private BigDecimal coutUnitairePrevisionnel;

    @Builder.Default
    private Boolean actif = true;
}
