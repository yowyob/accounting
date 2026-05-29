package com.yowyob.erp.accounting.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for configurable accounting operations.
 * Defines the main account, direction, and calculation rules for recurring
 * operations.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationComptableDto {

    private UUID id;

    @NotBlank(message = "Operation type cannot be empty")
    @Size(max = 50, message = "Operation type must not exceed 50 characters")
    @Pattern(regexp = "^(ACHAT|VENTE|SALAIRE|PAIEMENT|DIVERS)$", message = "Operation type must be ACHAT, VENTE, SALAIRE, PAIEMENT or DIVERS")
    @JsonProperty("typeOperation")
    private String type_operation;

    @NotBlank(message = "Settlement mode cannot be empty")
    @Size(max = 50, message = "Settlement mode must not exceed 50 characters")
    @Pattern(regexp = "^(ESPECE|CHEQUE|VIREMENT|MOBILE)$", message = "Settlement mode must be ESPECE, CHEQUE, VIREMENT or MOBILE")
    @JsonProperty("modeReglement")
    private String mode_reglement;

    @NotNull(message = "Main account ID cannot be null")
    @JsonProperty("comptePrincipalId")
    private UUID compte_principal_id;

    @NotNull(message = "Static account status cannot be null")
    @JsonProperty("estCompteStatique")
    private Boolean est_compte_statique;

    @NotBlank(message = "Main direction cannot be empty")
    @Pattern(regexp = "DEBIT|CREDIT", message = "Main direction must be DEBIT or CREDIT")
    @JsonProperty("sensPrincipal")
    private String sens_principal;

    @NotNull(message = "Journal ID cannot be null")
    @JsonProperty("journalComptableId")
    private UUID journal_comptable_id;

    @NotBlank(message = "Amount type cannot be empty")
    @Pattern(regexp = "HT|TTC|TVA|PAU", message = "Amount type must be HT, TTC, TVA or PAU")
    @JsonProperty("typeMontant")
    private String type_montant;

    @PositiveOrZero(message = "Client ceiling must be positive or zero")
    @JsonProperty("plafondClient")
    private BigDecimal plafond_client;

    @NotNull(message = "Active status cannot be null")
    private Boolean actif;

    @Size(max = 255, message = "Notes must not exceed 255 characters")
    private String notes;

    private List<ContrepartieDto> contreparties;

    @JsonProperty("createdAt")
    private LocalDateTime created_at;

    @JsonProperty("updatedAt")
    private LocalDateTime updated_at;

    @JsonProperty("createdBy")
    private String created_by;

    @JsonProperty("updatedBy")
    private String updated_by;
}