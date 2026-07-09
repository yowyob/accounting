package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegleIncorporationDto {
    private UUID id;

    @NotNull(message = "Le compte CG est obligatoire")
    private UUID compteCgId;

    @NotBlank(message = "Le numéro de compte CG est obligatoire")
    private String compteCgNo;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotBlank(message = "Le mode est obligatoire")
    private String mode;

    private BigDecimal tauxSubstitution;
    private BigDecimal montantSubstitution;
    private String baseCalcul;
    private String justification;
    private String compteEcart97;
    private UUID periodeId;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    @Builder.Default
    private Boolean hasEcritures = false;
}
