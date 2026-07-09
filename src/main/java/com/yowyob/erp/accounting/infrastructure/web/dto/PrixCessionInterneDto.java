package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrixCessionInterneDto {
    private UUID id;

    @NotNull(message = "Le centre cédant est obligatoire")
    private UUID centreCedantId;
    private String centreCedantLibelle;

    @NotNull(message = "Le centre bénéficiaire est obligatoire")
    private UUID centreBeneficiaireId;
    private String centreBeneficiaireLibelle;

    @NotBlank(message = "La prestation est obligatoire")
    private String prestationLibelle;

    @NotBlank(message = "La méthode de valorisation est obligatoire")
    private String methode;

    @NotNull(message = "Le prix unitaire est obligatoire")
    private BigDecimal prixUnitaire;

    @NotNull(message = "L'unité d'œuvre est obligatoire")
    private UUID uniteId;
    private String uniteLibelle;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    private LocalDate dateFin;

    @Builder.Default
    private Boolean hasImputations = false;

    private List<PrixCessionVersionDto> versions;
}
