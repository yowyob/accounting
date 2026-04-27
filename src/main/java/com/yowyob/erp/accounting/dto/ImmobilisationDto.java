package com.yowyob.erp.accounting.dto;

import jakarta.validation.constraints.NotBlank;
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
public class ImmobilisationDto {

    private UUID id;

    @NotBlank(message = "Le code est obligatoire")
    private String code;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "La date d'acquisition est obligatoire")
    private LocalDate dateAcquisition;

    @NotNull(message = "La valeur d'origine est obligatoire")
    @Positive(message = "La valeur d'origine doit être positive")
    private BigDecimal valeurOrigine;

    @Builder.Default
    private BigDecimal valeurResiduelle = BigDecimal.ZERO;

    @NotNull(message = "La durée de vie (en années) est obligatoire")
    @Positive(message = "La durée de vie doit être positive")
    private Integer dureeVie;

    /** LINEAIRE | DEGRESSIF | UNITES_PRODUCTION */
    @Builder.Default
    private String methodeAmortissement = "LINEAIRE";

    /** Pour DEGRESSIF : coefficient multiplicateur (ex: 1.5, 2.0, 2.5) */
    private BigDecimal coefficientDegressif;

    /** Pour UNITES_PRODUCTION : capacité totale de production */
    private BigDecimal capaciteTotaleProduction;

    @NotNull(message = "Le compte d'immobilisation est obligatoire")
    private UUID compteImmoId;

    @NotNull(message = "Le compte d'amortissement est obligatoire")
    private UUID compteAmortId;

    @NotNull(message = "Le compte de dotation est obligatoire")
    private UUID compteDotationId;

    /** ACTIF | CEDE | MISE_AU_REBUT */
    @Builder.Default
    private String statut = "ACTIF";

    // Cession
    private LocalDate dateCession;
    private BigDecimal prixCession;
    private UUID compteProductCessionId;
    private UUID compteVNCId;

    // Lecture seule
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
