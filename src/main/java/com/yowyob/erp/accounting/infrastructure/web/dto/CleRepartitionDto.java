package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CleRepartitionDto {
    private UUID id;

    @NotNull(message = "Le code est obligatoire")
    private String code;

    @NotNull(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "Le type est obligatoire")
    private String type; // FIXE, COUT_UNITAIRE, UNITE_OEUVRE

    @Builder.Default
    private Boolean actif = true;

    private List<CleRepartitionLigneDto> lignes;
}
