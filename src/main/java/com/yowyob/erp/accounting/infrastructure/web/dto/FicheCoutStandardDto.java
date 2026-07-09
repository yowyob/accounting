package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FicheCoutStandardDto {
    private UUID id;

    @NotBlank(message = "Le code produit est obligatoire")
    private String produitCode;

    @NotBlank(message = "Le libellé produit est obligatoire")
    private String produitLibelle;

    @NotNull(message = "La période de référence est obligatoire")
    private UUID periodeRefId;

    @NotBlank(message = "Le plan analytique est obligatoire")
    private String planAnalytiqueId;

    @Builder.Default
    private Boolean periodeCommencee = false;

    @Valid
    private List<LigneCoutStandardDto> lignes;
}
