// DTO pour les écritures comptables
package com.yowyob.erp.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcritureComptableDto {

    private UUID id;

    private String numeroEcriture;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "La date d'écriture est obligatoire")
    private LocalDate dateEcriture;

    @NotNull(message = "Le journal comptable est obligatoire")
    private UUID journalComptableId;

    private String journalComptableLibelle;

    @NotNull(message = "La période comptable est obligatoire")
    private UUID periodeComptableId;

    private String periodeComptableCode;

    @NotNull(message = "Le montant total Debit est obligatoire")
    private BigDecimal montantTotalDebit;

     @NotNull(message = "Le montant total Credit est obligatoire")
    private BigDecimal montantTotalCredit;
    
    private Boolean validee;

    private LocalDateTime dateValidation;

    private String validatedBy;

    private String referenceExterne;

    private String notes;

    private List<DetailEcritureDto> detailsEcriture;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}