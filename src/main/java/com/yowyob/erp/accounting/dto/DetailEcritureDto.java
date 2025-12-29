package com.yowyob.erp.accounting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for accounting entry details.
 * Used for creating or updating individual lines of an accounting entry.
 * Follows snake_case naming and English Javadoc as per development charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailEcritureDto {

    private UUID id;

    @NotNull(message = "The accounting entry ID is required")
    @JsonProperty("ecritureComptableId")
    private UUID ecriture_comptable_id;

    @NotNull(message = "The account ID is required")
    @JsonProperty("compteComptableId")
    private UUID compte_comptable_id;

    @NotBlank(message = "The label is required")
    private String libelle;

    @NotBlank(message = "The direction (sens) is required")
    private String sens;

    @Builder.Default
    @JsonProperty("montantDebit")
    private BigDecimal montant_debit = BigDecimal.ZERO;

    @Builder.Default
    @JsonProperty("montantCredit")
    private BigDecimal montant_credit = BigDecimal.ZERO;

    @Builder.Default
    private Boolean lettree = false;

    @JsonProperty("dateLettrage")
    private LocalDateTime date_lettrage;

    @Builder.Default
    private Boolean pointee = false;

    @JsonProperty("referenceBancaire")
    private String reference_bancaire;

    private String notes;

    @JsonProperty("dateEcriture")
    private LocalDateTime date_ecriture;

}