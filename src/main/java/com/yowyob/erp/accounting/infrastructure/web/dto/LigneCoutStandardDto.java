package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LigneCoutStandardDto {
    private UUID id;

    @NotBlank(message = "La composante est obligatoire")
    private String composante;

    private UUID centreId;
    private String centreLibelle;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "La quantité standard est obligatoire")
    private BigDecimal quantiteStandard;

    @NotNull(message = "Le coût unitaire standard est obligatoire")
    private BigDecimal coutUnitaireStandard;

    private BigDecimal coutStandardTotal;

    private BigDecimal activiteNormale;
}
