// DTO pour les détails d'écriture
package com.yowyob.erp.accounting.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailEcritureDto {

    private UUID id;

    @NotNull(message = "L'ID de l'écriture comptable est obligatoire")
    private UUID ecritureComptableId;

    @NotNull(message = "Le plan comptable est obligatoire")
    private UUID compteComptableId;



    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotBlank(message = "Le sens est obligatoire")
    private String sens;

    @Builder.Default
    private Double montantDebit = 0.0;

    @Builder.Default
    private Double montantCredit = 0.0;

    @Builder.Default
    private Boolean lettree = false;

    private LocalDateTime dateLettrage;
    @Builder.Default
    private Boolean pointee = false;
    private String referenceBancaire;

    private String notes;

    private LocalDateTime dateEcriture;

}