package com.yowyob.erp.accounting.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for counterparty entries in an accounting operation.
 * 
 * @author Leonel Delmat AZANGUE
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContrepartieDto {

    private UUID id;

    @NotNull(message = "Accounting operation ID is required")
    @JsonProperty("operationComptableId")
    private UUID operation_comptable_id;

    @NotNull(message = "Account ID is required")
    @JsonProperty("compteId")
    private UUID compte_id;

    @Builder.Default
    @NotNull(message = "Third-party account status is required")
    @JsonProperty("estCompteTiers")
    private Boolean est_compte_tiers = false;

    @NotBlank(message = "Direction is required")
    private String sens;

    @NotBlank(message = "Amount type is required")
    @JsonProperty("typeMontant")
    private String type_montant;

    @NotNull(message = "Journal is required")
    @JsonProperty("journalComptableId")
    private UUID journal_comptable_id;

    private String notes;

    @JsonProperty("createdAt")
    private LocalDateTime created_at;

    @JsonProperty("updatedAt")
    private LocalDateTime updated_at;
}