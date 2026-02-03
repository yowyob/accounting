package com.yowyob.erp.accounting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for Ecriture Comptable.
 * Follows snake_case naming as per Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcritureComptableDto {

    private UUID id;

    @JsonProperty("numeroEcriture")
    private String numero_ecriture;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "La date d'écriture est obligatoire")
    @JsonProperty("dateEcriture")
    private LocalDate date_ecriture;

    @NotNull(message = "Le journal comptable est obligatoire")
    @JsonProperty("journalComptableId")
    private UUID journal_comptable_id;

    @JsonProperty("journalComptableLibelle")
    private String journal_comptable_libelle;

    @NotNull(message = "La période comptable est obligatoire")
    @JsonProperty("periodeComptableId")
    private UUID periode_comptable_id;

    @JsonProperty("periodeComptableCode")
    private String periode_comptable_code;

    @NotNull(message = "Le montant total Debit est obligatoire")
    @JsonProperty("montantTotalDebit")
    private BigDecimal montant_total_debit;

    @NotNull(message = "Le montant total Credit est obligatoire")
    @JsonProperty("montantTotalCredit")
    private BigDecimal montant_total_credit;

    private Boolean validee;
    private String statut;
    private Boolean actif;

    @JsonProperty("dateValidation")
    private LocalDateTime date_validation;

    @JsonProperty("validatedBy")
    private String validated_by;

    @JsonProperty("referenceExterne")
    private String reference_externe;

    private String notes;

    @JsonProperty("detailsEcriture")
    private List<DetailEcritureDto> details_ecriture;

    @JsonProperty("createdAt")
    private LocalDateTime created_at;

    @JsonProperty("updatedAt")
    private LocalDateTime updated_at;
}