package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConfigurationAnalytiqueDto {
    @NotBlank(message = "La devise est obligatoire")
    @Builder.Default
    private String devise = "FCFA";

    @Min(0) @Max(4)
    @Builder.Default
    private Integer precision = 0;

    @Builder.Default
    private String separateurMilliers = " ";

    @Builder.Default
    private Boolean bloquerApresClotureCg = true;

    @Min(0) @Max(30)
    @Builder.Default
    private Integer joursGraceCloture = 5;

    @Builder.Default
    private Boolean autoriserSaisieRetroactive = false;

    @NotBlank(message = "La méthode de valorisation est obligatoire")
    @Builder.Default
    private String methodeValorisationStocks = "CUMP";

    @Builder.Default
    private Boolean importComptabiliteGeneraleActive = false;
}
